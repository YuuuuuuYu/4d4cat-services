package com.services.api.applydays.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.services.core.applydays.entity.Application;
import com.services.core.applydays.entity.VerificationRequest;
import com.services.core.applydays.entity.VerificationStatus;
import com.services.core.applydays.repository.ApplicationRepository;
import com.services.core.applydays.repository.NotificationQueueRepository;
import com.services.core.applydays.repository.VerificationImageRepository;
import com.services.core.applydays.repository.VerificationRequestRepository;
import com.services.core.applydays.service.ApplyDaysWorkerService;
import com.services.core.common.persistence.entity.Company;
import com.services.core.common.persistence.repository.CompanyRepository;
import com.services.core.fixture.ApplyDaysFixtures;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class AdminApplyDaysServiceTest {

  @Mock private ApplicationRepository applicationRepository;
  @Mock private VerificationRequestRepository verificationRequestRepository;
  @Mock private CompanyRepository companyRepository;
  @Mock private VerificationImageRepository verificationImageRepository;
  @Mock private ApplyDaysWorkerService applyDaysWorkerService;
  @Mock private NotificationQueueRepository notificationQueueRepository;
  @Mock private TransactionTemplate transactionTemplate;
  private MeterRegistry meterRegistry;

  private AdminApplyDaysCommandService adminApplyDaysCommandService;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    adminApplyDaysCommandService =
        new AdminApplyDaysCommandService(
            applicationRepository,
            verificationRequestRepository,
            verificationImageRepository,
            companyRepository,
            applyDaysWorkerService,
            notificationQueueRepository,
            meterRegistry,
            transactionTemplate);
  }

  @Test
  @DisplayName("인증 요청을 승인하면 즉시 승인 처리되고 알림 큐에 데이터가 저장된다")
  void approveRequest_success() {
    // given
    UUID requestId = UUID.randomUUID();
    UUID applicationId = UUID.randomUUID();
    String adminEmail = "admin@example.com";

    VerificationRequest request =
        VerificationRequest.builder()
            .applicationId(applicationId)
            .memberId(UUID.randomUUID())
            .build();

    Application application = ApplyDaysFixtures.createApplication("naver", 1L);
    ApplyDaysFixtures.setId(application, applicationId);

    when(verificationRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
    when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

    // when
    adminApplyDaysCommandService.approveRequest(requestId, null, null, adminEmail);

    // then
    verify(applyDaysWorkerService).processApproval(applicationId);
    verify(notificationQueueRepository).save(any());
    assertThat(meterRegistry.find("applydays.applications.approved").counter()).isNotNull();
    assertThat(meterRegistry.find("applydays.applications.approved").counter().count())
        .isEqualTo(1);
  }

  @Test
  @DisplayName("새로운 슬러그가 주어지면 모든 동일 슬러그 지원서의 슬러그를 업데이트한다")
  void approveRequest_with_new_slug_bulk_updates() {
    // given
    UUID requestId = UUID.randomUUID();
    UUID applicationId = UUID.randomUUID();
    String oldSlug = "old-slug";
    String newSlug = "new-slug";
    String adminEmail = "admin@example.com";

    VerificationRequest request =
        VerificationRequest.builder()
            .applicationId(applicationId)
            .memberId(UUID.randomUUID())
            .build();

    Application application = ApplyDaysFixtures.createApplication(oldSlug, 1L);
    ApplyDaysFixtures.setId(application, applicationId);
    Company company = ApplyDaysFixtures.createCompany();

    when(verificationRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
    when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
    when(companyRepository.findBySlug(oldSlug)).thenReturn(Optional.of(company));

    // when
    adminApplyDaysCommandService.approveRequest(requestId, newSlug, null, adminEmail);

    // then
    verify(applicationRepository).updateCompanySlug(oldSlug, newSlug);
  }

  @Test
  @DisplayName("인증 요청을 거절하면 Application 상태가 REJECTED가 되고 거절 사유가 기록된다")
  void rejectRequest_success() {
    // given
    UUID requestId = UUID.randomUUID();
    UUID applicationId = UUID.randomUUID();
    String adminEmail = "admin@example.com";
    String reason = "Image is not clear";

    VerificationRequest request =
        VerificationRequest.builder()
            .applicationId(applicationId)
            .memberId(UUID.randomUUID())
            .build();

    Application application = ApplyDaysFixtures.createApplication("naver", 1L);
    ApplyDaysFixtures.setId(application, applicationId);

    when(verificationRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
    when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

    // when
    adminApplyDaysCommandService.rejectRequest(requestId, reason, adminEmail);

    // then
    assertThat(request.getStatus()).isEqualTo(VerificationStatus.REJECTED);
    assertThat(request.getRejectionReason()).isEqualTo(reason);
    verify(notificationQueueRepository).save(any());
    assertThat(meterRegistry.find("applydays.applications.rejected").counter()).isNotNull();
    assertThat(meterRegistry.find("applydays.applications.rejected").counter().count())
        .isEqualTo(1);
  }
}
