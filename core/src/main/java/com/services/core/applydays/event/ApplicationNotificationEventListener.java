package com.services.core.applydays.event;

import com.services.core.applydays.entity.NotificationQueue;
import com.services.core.applydays.repository.NotificationQueueRepository;
import com.services.core.common.notification.discord.DiscordChannel;
import com.services.core.common.notification.discord.DiscordWebhookPayload;
import com.services.core.common.notification.discord.DiscordWebhookService;
import com.services.core.common.notification.discord.Embed;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.HtmlUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationNotificationEventListener {

  private final NotificationQueueRepository notificationQueueRepository;
  private final DiscordWebhookService discordWebhookService;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleApplicationApproved(ApplicationApprovedEvent event) {
    log.info("Handling ApplicationApprovedEvent for requestId={}", event.requestId());

    notificationQueueRepository.save(
        NotificationQueue.builder()
            .memberId(event.memberId())
            .applicationId(event.applicationId())
            .notificationType("APPROVAL")
            .scheduledAt(
                event.scheduledAt() != null
                    ? LocalDateTime.ofInstant(event.scheduledAt(), ZoneId.systemDefault())
                    : null)
            .build());

    if (discordWebhookService != null) {
      try {
        String description =
            String.format(
                "지원서 ID: %s\n회원 ID: %s\n상태: 승인 (APPROVED)",
                event.applicationId(), event.memberId());
        DiscordWebhookPayload payload =
            DiscordWebhookPayload.builder()
                .embeds(
                    List.of(Embed.builder().title("✅ 지원서 승인 완료").description(description).build()))
                .build();
        discordWebhookService.sendMessageAsync(payload, DiscordChannel.DEFAULT);
      } catch (Exception e) {
        log.error(
            "Failed to send Discord approval notification for requestId={}", event.requestId(), e);
      }
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleApplicationRejected(ApplicationRejectedEvent event) {
    log.info("Handling ApplicationRejectedEvent for requestId={}", event.requestId());

    notificationQueueRepository.save(
        NotificationQueue.builder()
            .memberId(event.memberId())
            .applicationId(event.applicationId())
            .notificationType("REJECTION")
            .build());

    if (discordWebhookService != null) {
      try {
        String sanitizedReason =
            HtmlUtils.htmlEscape(event.reason() != null ? event.reason() : "N/A");
        String description =
            String.format(
                "지원서 ID: %s\n회원 ID: %s\n거절 사유: %s",
                event.applicationId(), event.memberId(), sanitizedReason);
        DiscordWebhookPayload payload =
            DiscordWebhookPayload.builder()
                .embeds(
                    List.of(Embed.builder().title("❌ 지원서 거절 완료").description(description).build()))
                .build();
        discordWebhookService.sendMessageAsync(payload, DiscordChannel.DEFAULT);
      } catch (Exception e) {
        log.error(
            "Failed to send Discord rejection notification for requestId={}", event.requestId(), e);
      }
    }
  }
}
