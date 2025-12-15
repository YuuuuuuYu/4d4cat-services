package com.services.common.aop;

import com.services.common.application.exception.ErrorCode;
import com.services.common.infrastructure.discord.DiscordWebhookPayload;
import com.services.common.infrastructure.discord.Embed;
import com.services.common.infrastructure.discord.Footer;
import com.services.common.infrastructure.discord.application.DiscordWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializationAspect {

  private static final int DISCORD_COLOR_SUCCESS = 3066993; // 초록색
  private static final int DISCORD_COLOR_ERROR = 15158332; // 빨간색
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

    discordWebhookService.sendMessage(payload).subscribe();
  }

  private void sendErrorWebhook(String serviceName, ErrorCode errorCode, Exception exception) {
    String errorDescriptionFromCode =
        messageSource.getMessage(
            errorCode.getMessageKey(), new Object[] {exception.getMessage()}, DEFAULT_LOCALE);

    String errorTitle =
        messageSource.getMessage("discord.init.failure.title", null, DEFAULT_LOCALE);
    String finalDescription =
        String.format(
            "❌ 에러 코드: %s\n❌ 상세 내용: %s\n\n[MessageSource]: %s",
            errorCode.getCode(), exception.getMessage(), errorDescriptionFromCode);

    sendWebhookNotification(serviceName, errorTitle, finalDescription, DISCORD_COLOR_ERROR);
  }

  @Around(
      "execution(* com.services.common.application.DataInitializationService.setDataStorage(..))")
  public Object logDataInitialization(ProceedingJoinPoint joinPoint) throws Throwable {
    String serviceName = joinPoint.getTarget().getClass().getSimpleName();
    Instant startTime = Instant.now();

    log.info("🚀 Starting data initialization for {}", serviceName);

    try {
      Object result = joinPoint.proceed();
      Duration duration = Duration.between(startTime, Instant.now());
      String successTitle =
          messageSource.getMessage("discord.init.success.title", null, DEFAULT_LOCALE);
      String successDescription =
          messageSource.getMessage(
              "discord.init.success.description",
              new Object[] {serviceName, duration.toSeconds()},
              DEFAULT_LOCALE);

      log.info(successDescription);
      sendWebhookNotification(serviceName, successTitle, successDescription, DISCORD_COLOR_SUCCESS);

      return result;

    } catch (Exception e) {
      log.error("❌ Data Initialization Unexpected Error: {}", e.getMessage(), e);
      sendErrorWebhook(serviceName, ErrorCode.INTERNAL_SERVER_ERROR, e);
      throw e;
    }
  }
}
