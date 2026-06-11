package com.services.api.admin.email.service;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.services.api.admin.email.dto.ManualEmailRequest;
import com.services.core.common.notification.email.EmailNotificationService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminEmailServiceTest {

  @Mock private EmailNotificationService emailNotificationService;

  @InjectMocks private AdminEmailService adminEmailService;

  @Test
  @DisplayName("여러 이메일 주소로 각각 메일을 발송한다")
  void sendManualEmail_shouldIterateAndSend() {
    // given
    List<String> toEmails = List.of("user1@example.com", "user2@example.com");
    ManualEmailRequest request = new ManualEmailRequest(toEmails, "Test Subject", "Test Body");

    // when
    adminEmailService.sendManualEmail(request);

    // then
    verify(emailNotificationService, times(2))
        .sendEmail(anyString(), eq("User"), eq("Test Subject"), eq("Test Body"));

    verify(emailNotificationService)
        .sendEmail("user1@example.com", "User", "Test Subject", "Test Body");
    verify(emailNotificationService)
        .sendEmail("user2@example.com", "User", "Test Subject", "Test Body");
  }
}
