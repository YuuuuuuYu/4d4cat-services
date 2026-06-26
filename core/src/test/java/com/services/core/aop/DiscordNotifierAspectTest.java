package com.services.core.aop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.services.core.common.aop.DiscordNotifierAspect;
import com.services.core.common.notification.discord.DiscordChannel;
import com.services.core.common.notification.discord.DiscordWebhookPayload;
import com.services.core.common.notification.discord.DiscordWebhookService;
import com.services.core.common.notification.discord.NotifyDiscord;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(
    classes = {
      DiscordNotifierAspectTest.TestComponent.class,
      DiscordNotifierAspect.class,
      DiscordNotifierAspectTest.TestConfig.class
    })
@EnableAspectJAutoProxy
class DiscordNotifierAspectTest {

  @Configuration
  static class TestConfig {
    @Bean
    public MeterRegistry meterRegistry() {
      return new SimpleMeterRegistry();
    }
  }

  @Component
  static class TestComponent {
    @NotifyDiscord(taskName = "기본 작업")
    public void defaultMethod() {}

    @NotifyDiscord(taskName = "통계 작업", channel = DiscordChannel.STATISTICS)
    public void statisticsMethod() {}

    @Scheduled(cron = "0 0 * * * *")
    @NotifyDiscord(taskName = "스케줄 작업")
    public void scheduledMethod() {}

    @NotifyDiscord(taskName = "커스텀 작업", emoji = "🔥")
    public void customEmojiMethod() {}

    @NotifyDiscord(taskName = "실패 작업")
    public void failedMethod() {
      throw new RuntimeException("예상된 오류");
    }
  }

  @Autowired private TestComponent testComponent;

  @MockitoBean private DiscordWebhookService discordWebhookService;

  @Autowired private MeterRegistry meterRegistry;

  @Test
  @DisplayName("기본 작업인 경우 기본 emoji 🔔가 타이틀에 추가되는지 검증")
  void defaultMethod_shouldUseBellEmoji() {
    // When
    testComponent.defaultMethod();

    // Then
    ArgumentCaptor<DiscordWebhookPayload> captor =
        ArgumentCaptor.forClass(DiscordWebhookPayload.class);
    verify(discordWebhookService).sendMessageAsync(captor.capture(), any());

    String title = captor.getValue().getEmbeds().get(0).getTitle();
    assertEquals("🔔 ✅ 기본 작업 성공", title);
  }

  @Test
  @DisplayName("통계 작업인 경우 통계 emoji 📊가 타이틀에 추가되는지 검증")
  void statisticsMethod_shouldUseStatsEmoji() {
    // When
    testComponent.statisticsMethod();

    // Then
    ArgumentCaptor<DiscordWebhookPayload> captor =
        ArgumentCaptor.forClass(DiscordWebhookPayload.class);
    verify(discordWebhookService).sendMessageAsync(captor.capture(), any());

    String title = captor.getValue().getEmbeds().get(0).getTitle();
    assertEquals("📊 ✅ 통계 작업 성공", title);
  }

  @Test
  @DisplayName("스케줄 작업인 경우 스케줄 emoji ⏰가 타이틀에 추가되는지 검증")
  void scheduledMethod_shouldUseClockEmoji() {
    // When
    testComponent.scheduledMethod();

    // Then
    ArgumentCaptor<DiscordWebhookPayload> captor =
        ArgumentCaptor.forClass(DiscordWebhookPayload.class);
    verify(discordWebhookService).sendMessageAsync(captor.capture(), any());

    String title = captor.getValue().getEmbeds().get(0).getTitle();
    assertEquals("⏰ ✅ 스케줄 작업 성공", title);
  }

  @Test
  @DisplayName("커스텀 emoji가 지정된 경우 해당 emoji가 타이틀에 추가되는지 검증")
  void customEmojiMethod_shouldUseCustomEmoji() {
    // When
    testComponent.customEmojiMethod();

    // Then
    ArgumentCaptor<DiscordWebhookPayload> captor =
        ArgumentCaptor.forClass(DiscordWebhookPayload.class);
    verify(discordWebhookService).sendMessageAsync(captor.capture(), any());

    String title = captor.getValue().getEmbeds().get(0).getTitle();
    assertEquals("🔥 ✅ 커스텀 작업 성공", title);
  }

  @Test
  @DisplayName("작업 실패 시 🚨와 ❌ 및 해당 emoji가 타이틀에 추가되는지 검증")
  void failedMethod_shouldIncludeAlertAndErrorEmoji() {
    // When & Then
    assertThrows(RuntimeException.class, () -> testComponent.failedMethod());

    // Then
    ArgumentCaptor<DiscordWebhookPayload> captor =
        ArgumentCaptor.forClass(DiscordWebhookPayload.class);
    verify(discordWebhookService).sendMessageAsync(captor.capture(), any());

    String title = captor.getValue().getEmbeds().get(0).getTitle();
    assertEquals("🚨 🔔 ❌ 실패 작업 실패", title);
  }
}
