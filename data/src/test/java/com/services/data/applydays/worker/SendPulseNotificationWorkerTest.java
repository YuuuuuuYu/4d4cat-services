package com.services.data.applydays.worker;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.services.core.applydays.entity.Application;
import com.services.core.applydays.repository.VerificationRequestRepository;
import com.services.core.common.infrastructure.external.sendpulse.SendPulseEmailClient;
import com.services.core.common.infrastructure.external.sendpulse.dto.SendPulseEmailRequest;
import com.services.core.common.persistence.entity.Company;
import com.services.core.common.persistence.entity.member.Member;
import com.services.core.common.persistence.repository.CompanyRepository;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SendPulseNotificationWorkerTest {

  @Mock private SendPulseEmailClient sendPulseEmailClient;
  @Mock private CompanyRepository companyRepository;
  @Mock private VerificationRequestRepository verificationRequestRepository;
  private RateLimiter sendPulseRateLimiter;

  private SendPulseNotificationWorker worker;

  @BeforeEach
  void setUp() {
    sendPulseRateLimiter = RateLimiter.of("test", RateLimiterConfig.ofDefaults());
    worker =
        new SendPulseNotificationWorker(
            sendPulseEmailClient,
            companyRepository,
            verificationRequestRepository,
            sendPulseRateLimiter);
  }

  @Test
  @DisplayName("sendVerificationResultEmail - HTML 메일을 빌드하고 SendPulse 클라이언트를 호출한다")
  void sendVerificationResultEmail_shouldBuildHtmlAndSend() {
    // given
    ReflectionTestUtils.setField(worker, "senderEmail", "noreply@applydays.com");
    ReflectionTestUtils.setField(worker, "senderName", "ApplyDays");

    Member member = Member.builder().email("user@example.com").name("User").build();
    ReflectionTestUtils.setField(member, "id", UUID.randomUUID());

    Application app =
        Application.builder()
            .id(UUID.randomUUID())
            .companySlug("test-company")
            .positionDetail("Backend")
            .build();
    app.setAccessPassword("pass123");
    app.approve();

    Company company = Company.builder().slug("test-company").name("Test Co").build();
    when(companyRepository.findBySlug("test-company")).thenReturn(Optional.of(company));

    // when
    worker.sendVerificationResultEmail(member, List.of(app));

    // then
    verify(sendPulseEmailClient).sendEmail(any(SendPulseEmailRequest.class));
    verify(companyRepository).findBySlug("test-company");
  }
}
