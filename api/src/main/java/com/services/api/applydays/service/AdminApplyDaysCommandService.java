package com.services.api.applydays.service;

import com.services.api.applydays.dto.ApplicationRequest;
import com.services.api.applydays.dto.RejectionDetail;
import com.services.core.applydays.dto.HiringStepDetail;
import com.services.core.applydays.entity.Application;
import com.services.core.applydays.entity.NotificationQueue;
import com.services.core.applydays.entity.VerificationRequest;
import com.services.core.applydays.entity.VerificationStatus;
import com.services.core.applydays.repository.ApplicationRepository;
import com.services.core.applydays.repository.NotificationQueueRepository;
import com.services.core.applydays.repository.VerificationRequestRepository;
import com.services.core.applydays.service.ApplyDaysWorkerService;
import com.services.core.common.exception.ErrorCode;
import com.services.core.common.exception.NotFoundException;
import com.services.core.common.persistence.entity.CompanyStatus;
import com.services.core.common.persistence.repository.CompanyRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdminApplyDaysCommandService {

  private final ApplicationRepository applicationRepository;
  private final VerificationRequestRepository verificationRequestRepository;
  private final CompanyRepository companyRepository;
  private final ApplyDaysWorkerService applyDaysWorkerService;
  private final NotificationQueueRepository notificationQueueRepository;
  private final MeterRegistry meterRegistry;
  private final TransactionTemplate transactionTemplate;
  private final ExecutorService dbTaskExecutor = Executors.newVirtualThreadPerTaskExecutor();

  public void bulkApproveRequest(List<UUID> requestIds, String newSlug, Instant scheduledAt) {
    CompletableFuture.runAsync(
        () -> {
          for (UUID id : requestIds) {
            try {
              transactionTemplate.executeWithoutResult(
                  status -> processApprove(id, newSlug, scheduledAt));
            } catch (Exception e) {
              log.error("Failed to bulk approve request: {}", id, e);
            }
          }
        },
        dbTaskExecutor);
  }

  public void bulkRejectRequest(
      List<UUID> requestIds, String reason, List<RejectionDetail> details) {
    CompletableFuture.runAsync(
        () -> {
          if (details != null && !details.isEmpty()) {
            for (RejectionDetail detail : details) {
              try {
                transactionTemplate.executeWithoutResult(
                    status -> rejectRequest(detail.requestId(), detail.reason()));
              } catch (Exception e) {
                log.error("Failed to reject request: {}", detail.requestId(), e);
              }
            }
          } else if (requestIds != null) {
            for (UUID id : requestIds) {
              try {
                transactionTemplate.executeWithoutResult(status -> rejectRequest(id, reason));
              } catch (Exception e) {
                log.error("Failed to bulk reject request: {}", id, e);
              }
            }
          }
        },
        dbTaskExecutor);
  }

  public void asyncApproveRequest(UUID requestId, String newSlug, Instant scheduledAt) {
    CompletableFuture.runAsync(
        () -> {
          try {
            transactionTemplate.executeWithoutResult(
                status -> approveRequest(requestId, newSlug, scheduledAt));
          } catch (Exception e) {
            log.error("Failed to async approve request: {}", requestId, e);
          }
        },
        dbTaskExecutor);
  }

  public void asyncRejectRequest(UUID requestId, String reason) {
    CompletableFuture.runAsync(
        () -> {
          try {
            transactionTemplate.executeWithoutResult(status -> rejectRequest(requestId, reason));
          } catch (Exception e) {
            log.error("Failed to async reject request: {}", requestId, e);
          }
        },
        dbTaskExecutor);
  }

  public void updateApplication(UUID id, ApplicationRequest request) {
    Application application =
        applicationRepository
            .findById(id)
            .orElseThrow(() -> new NotFoundException(ErrorCode.APPLICATION_NOT_FOUND));

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
        } else {
          durationDays = 0;
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

    application.update(
        request.companySlug(),
        request.categoryId(),
        request.positionDetail(),
        request.appliedAt().toLocalDateTime(),
        processedProcess,
        request.channel());
  }

  public void approveRequest(UUID requestId, String newSlug, Instant scheduledAt) {
    processApprove(requestId, newSlug, scheduledAt);
    log.info("Request {} approved and notification queued in DB.", requestId);
  }

  public void processApprove(UUID requestId, String newSlug, Instant scheduledAt) {
    VerificationRequest request =
        verificationRequestRepository
            .findById(requestId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.APPLICATION_NOT_FOUND));

    if (request.getStatus() != VerificationStatus.PENDING) {
      log.warn("Request {} is not in PENDING status. Current: {}", requestId, request.getStatus());
      return;
    }

    applicationRepository
        .findById(request.getApplicationId())
        .ifPresent(
            app -> {
              String oldSlug = app.getCompanySlug();
              String finalSlug = (newSlug != null && !newSlug.isBlank()) ? newSlug : oldSlug;

              companyRepository
                  .findBySlug(oldSlug)
                  .ifPresent(
                      company -> {
                        if (!finalSlug.equals(oldSlug)) {
                          log.info("Updating company slug from {} to {}", oldSlug, finalSlug);
                          company.updateSlug(finalSlug);

                          applicationRepository.updateCompanySlug(oldSlug, finalSlug);
                          app.updateCompanySlug(finalSlug);
                        }

                        if (company.getStatus() == CompanyStatus.SUGGESTED) {
                          company.updateStatus(CompanyStatus.VERIFIED);
                          log.info("Updated company {} status to VERIFIED", finalSlug);
                        }
                      });
            });

    applyDaysWorkerService.processApproval(request.getApplicationId());

    notificationQueueRepository.save(
        NotificationQueue.builder()
            .memberId(request.getMemberId())
            .applicationId(request.getApplicationId())
            .notificationType("APPROVAL")
            .scheduledAt(
                scheduledAt != null
                    ? LocalDateTime.ofInstant(scheduledAt, ZoneId.systemDefault())
                    : null)
            .build());

    meterRegistry.counter("applydays.applications.approved").increment();
  }

  public void rejectRequest(UUID requestId, String reason) {
    VerificationRequest request =
        verificationRequestRepository
            .findById(requestId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.APPLICATION_NOT_FOUND));

    request.reject(reason);

    applicationRepository.findById(request.getApplicationId()).ifPresent(Application::reject);

    notificationQueueRepository.save(
        NotificationQueue.builder()
            .memberId(request.getMemberId())
            .applicationId(request.getApplicationId())
            .notificationType("REJECTION")
            .build());

    meterRegistry.counter("applydays.applications.rejected").increment();
  }

  public void deleteApplication(UUID id) {
    Application application =
        applicationRepository
            .findById(id)
            .orElseThrow(() -> new NotFoundException(ErrorCode.APPLICATION_NOT_FOUND));
    applicationRepository.delete(application);
    log.info("Application {} soft deleted by admin.", id);
  }
}
