package com.services.core.applydays.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.services.core.applydays.repository.NotificationQueueRepository;
import com.services.core.common.notification.discord.DiscordChannel;
import com.services.core.common.notification.discord.DiscordWebhookPayload;
import com.services.core.common.notification.discord.DiscordWebhookService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApplicationNotificationEventListenerTest {

  @Mock private NotificationQueueRepository notificationQueueRepository;
  @Mock private DiscordWebhookService discordWebhookService;

  private ApplicationNotificationEventListener eventListener;

  @BeforeEach
  void setUp() {
    eventListener =
        new ApplicationNotificationEventListener(
            notificationQueueRepository, discordWebhookService);
  }

  @Test
  @DisplayName("ApplicationApprovedEvent 수신 시 NotificationQueue 저장 및 디스코드 알림이 발송된다")
  void handleApplicationApproved_success() {
    // given
    UUID requestId = UUID.randomUUID();
    UUID memberId = UUID.randomUUID();
    UUID applicationId = UUID.randomUUID();
    Instant scheduledAt = Instant.now();

    ApplicationApprovedEvent event =
        new ApplicationApprovedEvent(requestId, memberId, applicationId, "test-slug", scheduledAt);

    // when
    eventListener.handleApplicationApproved(event);

    // then
    verify(notificationQueueRepository)
        .save(
            argThat(
                queue ->
                    queue.getMemberId().equals(memberId)
                        && queue.getApplicationId().equals(applicationId)
                        && "APPROVAL".equals(queue.getNotificationType())));

    verify(discordWebhookService)
        .sendMessageAsync(any(DiscordWebhookPayload.class), eq(DiscordChannel.DEFAULT));
  }

  @Test
  @DisplayName("ApplicationRejectedEvent 수신 시 NotificationQueue 저장 및 디스코드 알림이 발송된다")
  void handleApplicationRejected_success() {
    // given
    UUID requestId = UUID.randomUUID();
    UUID memberId = UUID.randomUUID();
    UUID applicationId = UUID.randomUUID();

    ApplicationRejectedEvent event =
        new ApplicationRejectedEvent(requestId, memberId, applicationId, "자격요건 미달");

    // when
    eventListener.handleApplicationRejected(event);

    // then
    verify(notificationQueueRepository)
        .save(
            argThat(
                queue ->
                    queue.getMemberId().equals(memberId)
                        && queue.getApplicationId().equals(applicationId)
                        && "REJECTION".equals(queue.getNotificationType())));

    verify(discordWebhookService)
        .sendMessageAsync(any(DiscordWebhookPayload.class), eq(DiscordChannel.DEFAULT));
  }
}
