package com.services.api.applydays.service;

import com.services.api.applydays.dto.CompanySummaryResponse;
import com.services.api.applydays.dto.MyApplicationResponse;
import com.services.core.applydays.dto.*;
import com.services.core.applydays.entity.*;
import com.services.core.applydays.repository.ApplicationRepository;
import com.services.core.applydays.repository.ApplyDaysStatisticsRepository;
import com.services.core.applydays.repository.CategoryRepository;
import com.services.core.applydays.repository.VerificationRequestRepository;
import com.services.core.common.dto.CompanyResponse;
import com.services.core.common.dto.PageResponse;
import com.services.core.common.exception.BadRequestException;
import com.services.core.common.exception.ErrorCode;
import com.services.core.common.exception.ForbiddenException;
import com.services.core.common.exception.NotFoundException;
import com.services.core.common.persistence.entity.Company;
import com.services.core.common.persistence.entity.member.Member;
import com.services.core.common.persistence.repository.CompanyRepository;
import com.services.core.common.persistence.repository.member.MemberRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ApplyDaysQueryService {

  private final ApplicationRepository applicationRepository;
  private final CompanyRepository companyRepository;
  private final CategoryRepository categoryRepository;
  private final MemberRepository memberRepository;
  private final VerificationRequestRepository verificationRequestRepository;
  private final ApplyDaysStatisticsRepository statisticsRepository;
  private final MeterRegistry meterRegistry;

  public Slice<? extends TimelineBasicResponse> getCompanyTimeline(
      Authentication authentication, String slug, Pageable pageable) {
    String authorityKey = getAuthorityKey(authentication);

    if ("SUBSCRIBER".equals(authorityKey)) {
      return applicationRepository.findTimelineDetailByCompanySlug(slug, pageable);
    }

    return new SliceImpl<>(List.of(), pageable, false);
  }

  @Cacheable(value = "companySearch", key = "#query")
  public List<CompanyResponse> searchCompanies(String query) {
    return companyRepository.searchByNameOrChosung(query);
  }

  @Cacheable(value = "companyList", key = "{#query, #pageable.pageNumber, #pageable.pageSize}")
  public PageResponse<CompanyListResponse> getCompanies(String query, Pageable pageable) {
    Pageable unsortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
    return PageResponse.of(companyRepository.findAllVerifiedWithStats(query, unsortedPageable));
  }

  @Cacheable(value = "categoryList")
  public List<Category> getCategories() {
    return categoryRepository.findAll();
  }

  public MyApplicationsSummaryResponse getMyApplicationsSummary(String email) {
    Member member =
        memberRepository
            .findByEmail(email)
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

    List<Object[]> results =
        applicationRepository.countByVerificationStatusForMember(member.getId());

    long totalCount = 0;
    long pendingCount = 0;
    long rejectedCount = 0;
    long approvedCount = 0;

    for (Object[] row : results) {
      VerificationStatus status = (VerificationStatus) row[0];
      long count = (long) row[1];
      totalCount += count;

      switch (status) {
        case PENDING -> pendingCount = count;
        case REJECTED -> rejectedCount = count;
        case APPROVED -> approvedCount = count;
      }
    }

    return MyApplicationsSummaryResponse.builder()
        .totalCount(totalCount)
        .pendingCount(pendingCount)
        .rejectedCount(rejectedCount)
        .approvedCount(approvedCount)
        .build();
  }

  public Page<MyApplicationResponse> getMyApplications(
      String email, VerificationStatus status, Pageable pageable) {
    Member member =
        memberRepository
            .findByEmail(email)
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

    Pageable sortedPageable = pageable;
    if (pageable.getSort().isUnsorted()) {
      sortedPageable =
          PageRequest.of(
              pageable.getPageNumber(), pageable.getPageSize(), Sort.by("appliedAt").descending());
    }

    Page<Application> myAppsPage =
        applicationRepository.findByMemberIdAndStatus(member.getId(), status, sortedPageable);

    List<String> slugs =
        myAppsPage.getContent().stream().map(Application::getCompanySlug).distinct().toList();

    Map<String, String> companyNameMap =
        companyRepository.findBySlugIn(slugs).stream()
            .collect(Collectors.toMap(Company::getSlug, Company::getName));

    List<UUID> applicationIds = myAppsPage.getContent().stream().map(Application::getId).toList();
    List<VerificationRequest> verificationRequests =
        applicationIds.isEmpty()
            ? List.of()
            : verificationRequestRepository.findByApplicationIdIn(applicationIds);
    Map<UUID, String> rejectionReasonMap =
        verificationRequests.stream()
            .filter(vr -> vr.getRejectionReason() != null)
            .collect(
                Collectors.toMap(
                    VerificationRequest::getApplicationId,
                    VerificationRequest::getRejectionReason));

    return myAppsPage.map(
        app ->
            MyApplicationResponse.of(
                app,
                companyNameMap.get(app.getCompanySlug()),
                rejectionReasonMap.getOrDefault(app.getId(), null)));
  }

  public ApplicationDetailResponse viewApplication(String email, UUID id, String password) {
    if (!StringUtils.hasText(password)) {
      throw new BadRequestException(ErrorCode.INVALID_REQUEST);
    }

    Application application =
        applicationRepository
            .findById(id)
            .orElseThrow(() -> new NotFoundException(ErrorCode.APPLICATION_NOT_FOUND));

    String decryptedPassword = application.getAccessPassword();
    if (decryptedPassword == null || !password.equals(decryptedPassword)) {
      throw new BadRequestException(ErrorCode.UNAUTHORIZED_APPLICATION_ACCESS);
    }

    Member member =
        memberRepository
            .findByEmail(email)
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

    VerificationRequest vr =
        verificationRequestRepository
            .findByApplicationId(id)
            .orElseThrow(() -> new NotFoundException(ErrorCode.APPLICATION_NOT_FOUND));

    if (!vr.getMemberId().equals(member.getId())) {
      throw new BadRequestException(ErrorCode.UNAUTHORIZED_APPLICATION_ACCESS);
    }

    meterRegistry.counter("applydays.applications.viewed").increment();

    Category category = categoryRepository.findById(application.getCategoryId()).orElse(null);
    String categoryName = category != null ? category.getName() : null;

    return ApplicationDetailResponse.from(application, categoryName);
  }

  @Cacheable(
      value = "companySummary",
      key =
          "#companySlug + '_' + T(com.services.api.applydays.service.ApplyDaysQueryService).getAuthorityKey(#authentication)")
  public CompanySummaryResponse getCompanySummary(
      Authentication authentication, String companySlug) {
    Company company =
        companyRepository
            .findBySlug(companySlug)
            .orElseThrow(() -> new NotFoundException(ErrorCode.COMPANY_NOT_FOUND));

    List<ApplyDaysStatistics> allStats = statisticsRepository.findAllByCompanySlug(companySlug);

    String authorityKey = getAuthorityKey(authentication);
    final boolean includeDetails = "SUBSCRIBER".equals(authorityKey);
    Map<Long, String> categoryMap = getCategoryMap();

    ApplyDaysStatistics companyEntity =
        allStats.stream()
            .filter(s -> "COMPANY".equals(s.getStatType()) && s.getCategoryId() == null)
            .findFirst()
            .orElse(null);

    ApplyDaysStatisticsResponse companyStats =
        companyEntity != null
            ? ApplyDaysStatisticsResponse.from(companyEntity, null, includeDetails)
            : null;

    List<ApplyDaysStatisticsResponse> l1Stats = List.of();
    List<ApplyDaysStatisticsResponse> l2Stats = List.of();

    boolean hasUserAccess =
        "USER".equals(authorityKey)
            || "REVIEWER".equals(authorityKey)
            || "SUBSCRIBER".equals(authorityKey);
    boolean hasReviewerAccess =
        "REVIEWER".equals(authorityKey) || "SUBSCRIBER".equals(authorityKey);

    if (hasUserAccess) {
      l1Stats =
          allStats.stream()
              .filter(s -> "CAT_L1".equals(s.getStatType()))
              .map(
                  s ->
                      ApplyDaysStatisticsResponse.from(
                          s, categoryMap.get(s.getCategoryId()), includeDetails))
              .toList();
    }
    if (hasReviewerAccess) {
      l2Stats =
          allStats.stream()
              .filter(s -> "CAT_L2".equals(s.getStatType()))
              .map(
                  s ->
                      ApplyDaysStatisticsResponse.from(
                          s, categoryMap.get(s.getCategoryId()), includeDetails))
              .toList();
    }

    return CompanySummaryResponse.builder()
        .slug(company.getSlug())
        .name(company.getName())
        .companyStats(companyStats)
        .categoryL1Stats(l1Stats)
        .categoryL2Stats(l2Stats)
        .build();
  }

  public List<ApplicationDetailResponse> getCompanyDetails(
      Authentication authentication, String companySlug) {
    String authorityKey = getAuthorityKey(authentication);

    if (!"SUBSCRIBER".equals(authorityKey)) {
      throw new ForbiddenException(ErrorCode.FORBIDDEN);
    }

    List<Application> applications =
        applicationRepository.findAllByCompanySlugAndVerificationStatus(
            companySlug, VerificationStatus.APPROVED);

    Map<Long, String> categoryMap = getCategoryMap();

    return applications.stream()
        .map(app -> ApplicationDetailResponse.from(app, categoryMap.get(app.getCategoryId())))
        .toList();
  }

  public static String getAuthorityKey(Authentication authentication) {
    if (authentication == null
        || !authentication.isAuthenticated()
        || (authentication instanceof AnonymousAuthenticationToken)) {
      return "ANONYMOUS";
    }
    Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
    boolean isSubscriber =
        authorities.stream()
            .anyMatch(
                a ->
                    a.getAuthority().equals("ROLE_SUBSCRIBER")
                        || a.getAuthority().equals("ROLE_ADMIN"));
    if (isSubscriber) {
      return "SUBSCRIBER";
    }
    boolean isReviewer =
        authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_REVIEWER"));
    if (isReviewer) {
      return "REVIEWER";
    }
    boolean isUser = authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER"));
    if (isUser) {
      return "USER";
    }
    return "AUTHENTICATED";
  }

  private Map<Long, String> getCategoryMap() {
    return getCategories().stream().collect(Collectors.toMap(Category::getId, Category::getName));
  }

  @Cacheable(value = "publicSummary")
  public PublicSummaryResponse getPublicSummary() {
    long totalReviews = applicationRepository.count();
    long totalCompanies = companyRepository.count();
    return PublicSummaryResponse.builder()
        .totalReviews(totalReviews)
        .totalCompanies(totalCompanies)
        .message("ApplyDays platform overview statistics")
        .build();
  }
}
