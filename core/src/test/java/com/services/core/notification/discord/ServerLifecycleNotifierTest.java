package com.services.core.notification.discord;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.services.core.common.notification.discord.DiscordChannel;
import com.services.core.common.notification.discord.DiscordWebhookService;
import com.services.core.common.notification.discord.ServerLifecycleNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ServerLifecycleNotifierTest {

  @Mock private DiscordWebhookService discordWebhookService;

  @Mock private Environment environment;

  @InjectMocks private ServerLifecycleNotifier serverLifecycleNotifier;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(serverLifecycleNotifier, "applicationName", "test-api");
    when(environment.getActiveProfiles()).thenReturn(new String[] {"local"});
  }

  @Test
  @DisplayName("서버 시작 시 Discord 알림이 정상적으로 비동기 발송되는지 검증")
  void onApplicationReady_shouldSendDiscordNotification() {
    // When
    serverLifecycleNotifier.onApplicationReady();

    // Then
    verify(discordWebhookService)
        .sendMessageAsync(
            argThat(
                payload ->
                    payload.getUsername().equals("System Lifecycle Bot")
                        && payload.getEmbeds().get(0).getTitle().contains("서버 시작 완료")
                        && payload.getEmbeds().get(0).getDescription().contains("test-api")),
            eq(DiscordChannel.MONITORING));
  }

  @Test
  @DisplayName("서버 종료 시 Discord 알림이 정상적으로 동기 발송되는지 검증")
  void onShutdown_shouldSendDiscordNotification() {
    // When
    serverLifecycleNotifier.onShutdown();

    // Then
    verify(discordWebhookService)
        .sendMessage(
            argThat(
                payload ->
                    payload.getUsername().equals("System Lifecycle Bot")
                        && payload.getEmbeds().get(0).getTitle().contains("서버 종료 중")
                        && payload.getEmbeds().get(0).getDescription().contains("test-api")),
            eq(DiscordChannel.MONITORING));
  }
}
