package com.services.api.applydays.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.services.api.applydays.dto.CompanySummaryResponse;
import com.services.api.applydays.dto.MyApplicationResponse;
import com.services.api.applydays.dto.MyApplicationsDashboardResponse;
import com.services.api.common.security.service.MemberService;
import com.services.core.applydays.dto.*;
import com.services.core.applydays.entity.*;
import com.services.core.applydays.repository.ApplicationRepository;
import com.services.core.applydays.repository.ApplicationSummary;
import com.services.core.applydays.repository.ApplyDaysStatisticsRepository;
import com.services.core.applydays.repository.CategoryRepository;
import com.services.core.applydays.repository.DashboardApplicationSummary;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplyDaysQueryService {

  private final ApplicationRepository applicationRepository;
  private final CompanyRepository companyRepository;
  private final CategoryRepository categoryRepository;
  private final MemberRepository memberRepository;
  private final VerificationRequestRepository verificationRequestRepository;
  private final ApplyDaysStatisticsRepository statisticsRepository;
  private final MeterRegistry meterRegistry;
  private final MemberService memberService;
  private final ObjectMapper objectMapper;

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

  public PageResponse<CompanyListResponse> getCompanies(
      Authentication authentication, String query, Pageable pageable) {
    PageResponse<CompanyListResponse> rawResponse = getRawCompanies(query, pageable);

    String authorityKey = getAuthorityKey(authentication);
    if ("ANONYMOUS".equals(authorityKey)) {
      List<CompanyListResponse> maskedContent =
          rawResponse.getContent().stream()
              .map(
                  company ->
                      CompanyListResponse.builder()
                          .slug(company.getSlug())
                          .name(company.getName())
                          .reviewCount(company.getReviewCount())
                          .ghostingCount(null)
                          .ghostingRate(null)
                          .avgResponseTime(null)
                          .build())
              .toList();
      return new PageResponse<>(maskedContent, rawResponse.isHasNext());
    }

    return rawResponse;
  }

  @Cacheable(value = "companyList", key = "{#query, #pageable.pageNumber, #pageable.pageSize}")
  public PageResponse<CompanyListResponse> getRawCompanies(String query, Pageable pageable) {
    Pageable unsortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
    return PageResponse.of(companyRepository.findAllVerifiedWithStats(query, unsortedPageable));
  }

  @Cacheable(value = "categoryList")
  public List<Category> getCategories() {
    return categoryRepository.findAllByOrderByNameAsc();
  }

  public MyApplicationsSummaryResponse getMyApplicationsSummary(String email) {
    UUID memberId = UUID.fromString(memberService.getMemberIdByEmail(email));
    return getMyApplicationsSummaryByMemberId(memberId);
  }

  private MyApplicationsSummaryResponse getMyApplicationsSummaryByMemberId(UUID memberId) {
    List<Object[]> results = applicationRepository.countByVerificationStatusForMember(memberId);

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
    UUID memberId = UUID.fromString(memberService.getMemberIdByEmail(email));
    return getMyApplicationsByMemberId(memberId, status, pageable);
  }

  private Page<MyApplicationResponse> getMyApplicationsByMemberId(
      UUID memberId, VerificationStatus status, Pageable pageable) {
    Pageable sortedPageable = pageable;
    if (pageable.getSort().isUnsorted()) {
      sortedPageable =
          PageRequest.of(
              pageable.getPageNumber(), pageable.getPageSize(), Sort.by("appliedAt").descending());
    }

    Page<ApplicationSummary> myAppsPage =
        applicationRepository.findByMemberIdAndStatus(memberId, status, sortedPageable);

    List<String> slugs =
        myAppsPage.getContent().stream()
            .map(ApplicationSummary::getCompanySlug)
            .distinct()
            .toList();

    Map<String, String> companyNameMap =
        companyRepository.findBySlugIn(slugs).stream()
            .collect(Collectors.toMap(Company::getSlug, Company::getName));

    List<UUID> applicationIds =
        myAppsPage.getContent().stream().map(ApplicationSummary::getId).toList();
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

  public MyApplicationsDashboardResponse getMyApplicationsDashboard(
      String email, Pageable pageable) {
    UUID memberId = UUID.fromString(memberService.getMemberIdByEmail(email));

    Pageable sortedPageable = pageable;
    if (pageable.getSort().isUnsorted()) {
      sortedPageable =
          PageRequest.of(
              pageable.getPageNumber(), pageable.getPageSize(), Sort.by("appliedAt").descending());
    }

    // 1. 요약(summary) 조회 (1회 쿼리)
    MyApplicationsSummaryResponse summary = getMyApplicationsSummaryByMemberId(memberId);

    // 2. 대시보드 대상 애플리케이션 일괄 조회 (1회 쿼리 - 윈도우 함수 적용)
    List<DashboardApplicationSummary> dashboardApps =
        applicationRepository.findDashboardApplications(memberId);

    // 3. 일괄 조회한 데이터에서 slugs와 applicationIds 추출
    List<String> slugs =
        dashboardApps.stream().map(DashboardApplicationSummary::getCompanySlug).distinct().toList();
    List<UUID> applicationIds =
        dashboardApps.stream().map(DashboardApplicationSummary::getId).toList();

    // 4. Company 및 VerificationRequest 일괄 In-Query 조회 (각 1회씩, 총 2회 쿼리)
    Map<String, String> companyNameMap =
        slugs.isEmpty()
            ? Map.of()
            : companyRepository.findBySlugIn(slugs).stream()
                .collect(Collectors.toMap(Company::getSlug, Company::getName, (v1, v2) -> v1));

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
                    VerificationRequest::getRejectionReason,
                    (v1, v2) -> v1));

    // 5. MyApplicationResponse로 일괄 변환 및 상태별 필터링
    List<MyApplicationResponse> allResponses =
        dashboardApps.stream()
            .map(
                app -> {
                  List<HiringStepDetail> hiringSteps = List.of();
                  if (StringUtils.hasText(app.getHiringProcess())) {
                    try {
                      hiringSteps =
                          objectMapper.readValue(
                              app.getHiringProcess(),
                              new TypeReference<List<HiringStepDetail>>() {});
                    } catch (JsonProcessingException e) {
                      log.error(
                          "Failed to parse hiring process JSON for application ID: {}",
                          app.getId(),
                          e);
                    }
                  }
                  String companyName = companyNameMap.getOrDefault(app.getCompanySlug(), null);
                  return MyApplicationResponse.builder()
                      .id(app.getId())
                      .companySlug(app.getCompanySlug())
                      .companyName(companyName != null ? companyName : app.getCompanySlug())
                      .categoryId(app.getCategoryId())
                      .positionDetail(app.getPositionDetail())
                      .appliedAt(app.getAppliedAt())
                      .hiringProcess(hiringSteps)
                      .verificationStatus(app.getVerificationStatus())
                      .rejectionReason(rejectionReasonMap.getOrDefault(app.getId(), null))
                      .channel(app.getChannel())
                      .build();
                })
            .toList();

    List<MyApplicationResponse> pendingList =
        allResponses.stream()
            .filter(app -> app.verificationStatus() == VerificationStatus.PENDING)
            .toList();

    List<MyApplicationResponse> approvedList =
        allResponses.stream()
            .filter(app -> app.verificationStatus() == VerificationStatus.APPROVED)
            .toList();

    List<MyApplicationResponse> rejectedList =
        allResponses.stream()
            .filter(app -> app.verificationStatus() == VerificationStatus.REJECTED)
            .toList();

    // 6. PageResponse 조립 (totalCount는 summary에서 가져옴)
    Page<MyApplicationResponse> pendingPage =
        new PageImpl<>(pendingList, sortedPageable, summary.pendingCount());

    Page<MyApplicationResponse> approvedPage =
        new PageImpl<>(approvedList, sortedPageable, summary.approvedCount());

    Page<MyApplicationResponse> rejectedPage =
        new PageImpl<>(rejectedList, sortedPageable, summary.rejectedCount());

    return MyApplicationsDashboardResponse.builder()
        .summary(summary)
        .pendingApplications(PageResponse.of(pendingPage))
        .approvedApplications(PageResponse.of(approvedPage))
        .rejectedApplications(PageResponse.of(rejectedPage))
        .build();
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
    Map<Long, String> categoryMap = getCategoryMap();

    boolean hasUserAccess =
        "USER".equals(authorityKey)
            || "REVIEWER".equals(authorityKey)
            || "SUBSCRIBER".equals(authorityKey);
    boolean hasReviewerAccess =
        "REVIEWER".equals(authorityKey) || "SUBSCRIBER".equals(authorityKey);

    ApplyDaysStatistics companyEntity =
        allStats.stream()
            .filter(s -> "COMPANY".equals(s.getStatType()) && s.getCategoryId() == null)
            .findFirst()
            .orElse(null);

    ApplyDaysStatisticsResponse companyStats =
        companyEntity != null
            ? ApplyDaysStatisticsResponse.from(companyEntity, null, hasUserAccess)
            : null;

    List<ApplyDaysStatisticsResponse> l1Stats = List.of();
    List<ApplyDaysStatisticsResponse> l2Stats = List.of();

    if (hasUserAccess) {
      l1Stats =
          allStats.stream()
              .filter(s -> "CAT_L1".equals(s.getStatType()))
              .map(
                  s ->
                      ApplyDaysStatisticsResponse.from(
                          s, categoryMap.get(s.getCategoryId()), hasUserAccess))
              .toList();
    }
    if (hasReviewerAccess) {
      l2Stats =
          allStats.stream()
              .filter(s -> "CAT_L2".equals(s.getStatType()))
              .map(
                  s ->
                      ApplyDaysStatisticsResponse.from(
                          s, categoryMap.get(s.getCategoryId()), hasReviewerAccess))
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

    List<ApplicationSummary> applications =
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
