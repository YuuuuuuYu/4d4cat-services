package com.services.api.aop;

import com.services.api.discord.DiscordWebhookPayload;
import com.services.api.discord.DiscordWebhookService;
import com.services.api.discord.Embed;
import com.services.api.discord.Footer;
import com.services.api.util.WebUtils;
import com.services.core.exception.CustomException;
import com.services.core.exception.ErrorCode;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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

  private static final int DISCORD_COLOR_SUCCESS = 3066993;
  private static final int DISCORD_COLOR_ERROR = 15158332;
  private static final String BOT_USERNAME = "Application Event Bot";
  private static final Locale DEFAULT_LOCALE = Locale.getDefault();

  private final DiscordWebhookService discordWebhookService;
  private final MessageSource messageSource;

  private void sendWebhookNotification(
      String serviceName, String title, String description, int color) {
    Footer footer = Footer.builder().text("Service: " + serviceName).build();

    Embed embed =
        Embed.builder()
            .title(title)
            .description(description)
            .color(color)
            .timestamp(Instant.now().toString())
            .footer(footer)
            .build();

    DiscordWebhookPayload payload =
        DiscordWebhookPayload.builder().username(BOT_USERNAME).embeds(Arrays.asList(embed)).build();

    discordWebhookService.sendMessageAsync(payload);
  }

  private void sendErrorWebhook(
      String serviceName, String taskName, Exception exception, String clientIp) {
    ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
    if (exception instanceof CustomException customException) {
      errorCode = customException.getErrorCode();
    }

    String errorTitle =
        messageSource.getMessage(
            "discord.init.failure.title", new Object[] {taskName}, DEFAULT_LOCALE);
    String finalDescription =
        String.format(
            "ErrorCode: `%s`\nMessage: `%s`\nClient IP: `%s`",
            errorCode.getCode(), exception.getMessage(), clientIp);

    sendWebhookNotification(serviceName, errorTitle, finalDescription, DISCORD_COLOR_ERROR);
  }

  @Around("@annotation(notifyDiscord)")
  public Object notifyEvent(ProceedingJoinPoint joinPoint, NotifyDiscord notifyDiscord)
      throws Throwable {
    String serviceName = joinPoint.getTarget().getClass().getSimpleName();
    String taskName = notifyDiscord.taskName();
    Instant startTime = Instant.now();

    String clientIp = WebUtils.getClientIp();
    String startLogMessage =
        StringUtils.isNotBlank(notifyDiscord.startLog())
            ? notifyDiscord.startLog()
            : String.format("Starting task '%s' in %s (IP: %s)", taskName, serviceName, clientIp);
    log.info(startLogMessage);

    try {
      Object result = joinPoint.proceed();
      Duration duration = Duration.between(startTime, Instant.now());
      String successTitle =
          messageSource.getMessage(
              "discord.init.success.title", new Object[] {taskName}, DEFAULT_LOCALE);
      String baseDescription =
          messageSource.getMessage(
              "discord.init.success.description",
              new Object[] {taskName, duration.toSeconds()},
              DEFAULT_LOCALE);
      String successDescription = baseDescription + "\nClient IP: `" + clientIp + "`";

      log.info(successDescription);
      sendWebhookNotification(serviceName, successTitle, successDescription, DISCORD_COLOR_SUCCESS);

      return result;

    } catch (Exception e) {
      String errorLogMessage =
          StringUtils.isNotBlank(notifyDiscord.errorLog())
              ? String.format(notifyDiscord.errorLog(), e.getMessage())
              : String.format("Task '%s' in %s failed", taskName, serviceName);
      log.error(errorLogMessage, e);

      sendErrorWebhook(serviceName, taskName, e, clientIp);
      throw e;
    }
  }
}
