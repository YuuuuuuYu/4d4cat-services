package com.services.data.common.worker;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.services.core.common.infrastructure.RedisMessageQueue;
import com.services.core.common.notification.email.EmailNotificationService;
import com.services.core.common.notification.email.dto.EmailSendEvent;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EmailSendEventListenerTest {

  @Mock private RedisMessageQueue redisMessageQueue;
  @Mock private EmailNotificationService emailNotificationService;

  @InjectMocks private EmailSendEventListener emailSendEventListener;

  @Test
  @DisplayName("processScheduledEmails - Redis에서 메시지를 가져와 이메일을 발송한다")
  void processScheduledEmails_shouldFetchAndSend() {
    // given
    ReflectionTestUtils.setField(emailSendEventListener, "emailQueueName", "test-queue");

    EmailSendEvent event =
        EmailSendEvent.builder()
            .toEmail("test@example.com")
            .toNickname("Tester")
            .subject("Test Subject")
            .content("Test Content")
            .build();

    when(redisMessageQueue.zPopByScore(eq("test-queue"), anyDouble())).thenReturn(List.of(event));

    // when
    emailSendEventListener.processScheduledEmails();

    // then
    verify(emailNotificationService)
        .sendEmail("test@example.com", "Tester", "Test Subject", "Test Content");
  }
}
