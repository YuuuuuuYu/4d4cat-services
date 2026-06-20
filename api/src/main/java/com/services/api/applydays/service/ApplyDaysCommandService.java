package com.services.api.applydays.service;

import com.services.api.applydays.dto.ApplicationRequest;
import com.services.core.applydays.dto.HiringStepDetail;
import com.services.core.applydays.entity.Application;
import com.services.core.applydays.entity.Category;
import com.services.core.applydays.entity.VerificationRequest;
import com.services.core.applydays.repository.ApplicationRepository;
import com.services.core.applydays.repository.CategoryRepository;
import com.services.core.applydays.repository.VerificationImageRepository;
import com.services.core.applydays.repository.VerificationRequestRepository;
import com.services.core.common.exception.ErrorCode;
import com.services.core.common.exception.ForbiddenException;
import com.services.core.common.exception.NotFoundException;
import com.services.core.common.persistence.entity.Company;
import com.services.core.common.persistence.entity.CompanyStatus;
import com.services.core.common.persistence.entity.member.Member;
import com.services.core.common.persistence.repository.CompanyRepository;
import com.services.core.common.persistence.repository.member.MemberRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ApplyDaysCommandService {

  private final ApplicationRepository applicationRepository;
  private final CompanyRepository companyRepository;
  private final CategoryRepository categoryRepository;
  private final MemberRepository memberRepository;
  private final VerificationRequestRepository verificationRequestRepository;
  private final VerificationImageRepository verificationImageRepository;
  private final MeterRegistry meterRegistry;

  public UUID registerApplication(String email, ApplicationRequest request) {
    log.info("Registering application for user: {}", email);

    Member member =
        memberRepository
            .findByEmail(email)
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

    Optional<Company> companyOpt = companyRepository.findBySlug(request.companySlug());
    if (companyOpt.isEmpty() && StringUtils.hasText(request.companyName())) {
      companyOpt = companyRepository.findByName(request.companyName());
    }

    String finalCompanySlug;
    if (companyOpt.isPresent()) {
      finalCompanySlug = companyOpt.get().getSlug();
    } else {
      String suggestedSlug =
          StringUtils.hasText(request.companySlug())
              ? request.companySlug()
              : request.companyName();
      String name =
          StringUtils.hasText(request.companyName()) ? request.companyName() : suggestedSlug;

      Company suggestedCompany =
          Company.builder().slug(suggestedSlug).name(name).status(CompanyStatus.SUGGESTED).build();

      companyRepository.save(suggestedCompany);
      finalCompanySlug = suggestedSlug;
      log.info("Registered new suggested company: {} (slug: {})", name, suggestedSlug);
    }

    Category category =
        categoryRepository
            .findById(request.categoryId())
            .orElseThrow(() -> new NotFoundException(ErrorCode.CATEGORY_NOT_FOUND));

    UUID appId = UUID.randomUUID();

    List<HiringStepDetail> processedProcess = new ArrayList<>();
    if (request.hiringProcess() != null) {
      LocalDateTime lastDate = request.appliedAt().toLocalDateTime();
      for (HiringStepDetail step : request.hiringProcess()) {
        Integer durationDays = 0;
        if (step.stepDate() != null && !"GHOSTED".equals(step.status())) {
          try {
            LocalDate stepDate = LocalDate.parse(step.stepDate());
            long days = ChronoUnit.DAYS.between(lastDate.toLocalDate(), stepDate);
            durationDays = (int) Math.max(0, days);
            lastDate = stepDate.atStartOfDay();
          } catch (Exception e) {
            log.warn("Failed to parse stepDate: {}", step.stepDate());
          }
        }
        processedProcess.add(
            HiringStepDetail.builder()
                .stepType(step.stepType())
                .stepDate(step.stepDate())
                .status(step.status())
                .durationDays(durationDays)
                .build());
      }
    }

    Application application =
        Application.builder()
            .id(appId)
            .companySlug(finalCompanySlug)
            .categoryId(category.getId())
            .positionDetail(request.positionDetail())
            .appliedAt(request.appliedAt().toLocalDateTime())
            .hiringProcess(processedProcess)
            .channel(request.channel())
            .build();

    Application saved = applicationRepository.save(application);

    meterRegistry
        .counter(
            "applydays.applications.registered",
            "channel",
            request.channel().name(),
            "category",
            String.valueOf(request.categoryId()))
        .increment();

    VerificationRequest verificationRequest =
        VerificationRequest.builder().applicationId(saved.getId()).memberId(member.getId()).build();
    verificationRequestRepository.save(verificationRequest);

    return saved.getId();
  }

  public void deleteApplication(String email, UUID id) {
    deleteApplications(email, List.of(id));
  }

  public void deleteApplications(String email, List<UUID> ids) {
    if (ids == null || ids.isEmpty()) {
      return;
    }

    Member member =
        memberRepository
            .findByEmail(email)
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

    List<VerificationRequest> verificationRequests =
        verificationRequestRepository.findByApplicationIdIn(ids);

    if (verificationRequests.size() < ids.size()) {
      throw new NotFoundException(ErrorCode.APPLICATION_NOT_FOUND);
    }

    boolean hasUnauthorized =
        verificationRequests.stream().anyMatch(vr -> !vr.getMemberId().equals(member.getId()));
    if (hasUnauthorized) {
      throw new ForbiddenException(ErrorCode.UNAUTHORIZED_APPLICATION_ACCESS);
    }

    List<UUID> targetAppIds =
        verificationRequests.stream().map(VerificationRequest::getApplicationId).toList();

    List<Application> apps = applicationRepository.findAllById(targetAppIds);
    if (apps.size() < targetAppIds.size()) {
      throw new NotFoundException(ErrorCode.APPLICATION_NOT_FOUND);
    }

    applicationRepository.deleteAll(apps);
    targetAppIds.forEach(
        appId -> meterRegistry.counter("applydays.applications.deleted").increment());
  }
}
