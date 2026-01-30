package com.services.core.aop;

import com.services.core.exception.CustomException;
import com.services.core.exception.ErrorCode;
import com.services.core.notification.DataCollectionResult;
import com.services.core.notification.discord.DiscordWebhookPayload;
import com.services.core.notification.discord.DiscordWebhookService;
import com.services.core.notification.discord.Embed;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class DiscordNotifierAspect {

  private static final int DISCORD_COLOR_SUCCESS = 3066993; // Green
  private static final int DISCORD_COLOR_WARNING = 16734208; // Orange
  private static final int DISCORD_COLOR_ERROR = 15158332; // Red
  private static final String BOT_USERNAME = "Application Event Bot";

  private final DiscordWebhookService discordWebhookService;
  private final Optional<MessageSource> messageSource; // Optional dependency

  @Around("@annotation(notifyDiscord)")
  public Object notifyEvent(ProceedingJoinPoint joinPoint, NotifyDiscord notifyDiscord)
      throws Throwable {
    String serviceName = joinPoint.getSignature().getDeclaringType().getSimpleName();
    String taskName = notifyDiscord.taskName();
    Instant startTime = Instant.now();

    log.info("Starting task: '{}' in {}", taskName, serviceName);

    try {
      Object result = joinPoint.proceed();
      Duration duration = Duration.between(startTime, Instant.now());
      log.info("Task '{}' completed successfully in {} seconds.", taskName, duration.toSeconds());

      sendSuccessWebhook(serviceName, taskName, duration, result);

      return result;

    } catch (Throwable e) {
      log.error("Task '{}' in {} failed.", taskName, serviceName, e);
      sendErrorWebhook(serviceName, taskName, e);
      throw e;
    }
  }

  private void sendSuccessWebhook(
      String serviceName, String taskName, Duration duration, Object result) {
    String title = String.format("✅ %s 성공", taskName);
    String description;
    int color = DISCORD_COLOR_SUCCESS;

    if (result instanceof DataCollectionResult res) {
      description =
          String.format(
              "**총 %d개**의 아이템이 저장되었습니다.\n"
                  + "• **성공 필터:** %d개\n"
                  + "• **실패 필터:** %d개\n"
                  + "• **소요 시간:** %.2f초",
              res.totalItems(), res.successFilters(), res.failedFilters(), res.durationSeconds());
      if (res.failedFilters() > 0) {
        color = DISCORD_COLOR_WARNING; // Set color to warning if there are failures
      }
    } else {
      description = String.format("작업이 %.2f초 만에 성공적으로 완료되었습니다.", duration.toSeconds() / 1000.0);
    }

    sendWebhookNotification(serviceName, title, description, color);
  }

  private void sendErrorWebhook(String serviceName, String taskName, Throwable exception) {
    String title = String.format("❌ %s 실패", taskName);
    ErrorCode errorCode =
        (exception instanceof CustomException customException)
            ? customException.getErrorCode()
            : ErrorCode.INTERNAL_SERVER_ERROR;

    String description =
        String.format(
            "**ErrorCode:** `%s`\n**Message:** `%s`", errorCode.getCode(), exception.getMessage());

    // Use MessageSource if available, otherwise use default title
    messageSource.ifPresent(
        ms -> {
          String localizedTitle =
              ms.getMessage(
                  "discord.failure.title", new Object[] {taskName}, title, Locale.getDefault());
          sendWebhookNotification(serviceName, localizedTitle, description, DISCORD_COLOR_ERROR);
        });

    if (messageSource.isEmpty()) {
      sendWebhookNotification(serviceName, title, description, DISCORD_COLOR_ERROR);
    }
  }

  private void sendWebhookNotification(
      String serviceName, String title, String description, int color) {

    Embed embed =
        Embed.builder()
            .title(title)
            .description(description)
            .color(color)
            .timestamp(Instant.now().toString())
            .build();

    DiscordWebhookPayload payload =
        DiscordWebhookPayload.builder().username(BOT_USERNAME).embeds(List.of(embed)).build();

    discordWebhookService.sendMessageAsync(payload);
  }
}
