package com.services.core.common.notification.discord;

import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "discord.lifecycle", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class ServerLifecycleNotifier {

  private final DiscordWebhookService discordWebhookService;
  private final Environment environment;

  @Value("${spring.application.name:unknown}")
  private String applicationName;

  @EventListener(ApplicationReadyEvent.class)
  public void onApplicationReady() {
    sendNotification("🚀 서버 시작 완료", "애플리케이션이 성공적으로 시작되어 요청을 받을 준비가 되었습니다.", 3066993);
  }

  @PreDestroy
  public void onShutdown() {
    sendNotificationSync("🛑 서버 종료 중", "애플리케이션이 안전하게 종료(Graceful Shutdown) 절차를 수행 중입니다.", 15158332);
  }

  private void sendNotification(String title, String message, int color) {
    try {
      DiscordWebhookPayload payload = buildPayload(title, message, color);
      discordWebhookService.sendMessageAsync(payload, DiscordChannel.MONITORING);
    } catch (Exception e) {
      log.error("Failed to send startup notification: {}", e.getMessage());
    }
  }

  private void sendNotificationSync(String title, String message, int color) {
    try {
      DiscordWebhookPayload payload = buildPayload(title, message, color);
      discordWebhookService.sendMessage(payload, DiscordChannel.MONITORING);
    } catch (Exception e) {
      log.error("Failed to send shutdown notification: {}", e.getMessage());
    }
  }

  private DiscordWebhookPayload buildPayload(String title, String message, int color) {
    String activeProfiles = String.join(", ", environment.getActiveProfiles());

    String description =
        String.format(
            "**Service:** `%s`\n" + "**Profile:** `%s`\n" + "**Time:** %s\n\n" + "%s",
            applicationName, activeProfiles, Instant.now().toString(), message);

    Embed embed =
        Embed.builder()
            .title(title)
            .description(description)
            .color(color)
            .timestamp(Instant.now().toString())
            .build();

    return DiscordWebhookPayload.builder()
        .username("System Lifecycle Bot")
        .embeds(List.of(embed))
        .build();
  }
}
