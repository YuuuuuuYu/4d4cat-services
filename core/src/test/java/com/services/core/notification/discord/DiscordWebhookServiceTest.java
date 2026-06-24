package com.services.core.notification.discord;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.services.core.common.notification.discord.DiscordChannel;
import com.services.core.common.notification.discord.DiscordProperties;
import com.services.core.common.notification.discord.DiscordWebhookPayload;
import com.services.core.common.notification.discord.DiscordWebhookService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

class DiscordWebhookServiceTest {

  private RestClient.Builder restClientBuilder;
  private RestClient restClient;
  private RestClient.RequestBodyUriSpec requestBodyUriSpec;
  private RestClient.RequestBodySpec requestBodySpec;
  private RestClient.ResponseSpec responseSpec;

  private DiscordProperties discordProperties;
  private MeterRegistry meterRegistry;
  private DiscordWebhookService discordWebhookService;

  @BeforeEach
  void setUp() {
    restClientBuilder = mock(RestClient.Builder.class);
    restClient = mock(RestClient.class);
    requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
    requestBodySpec = mock(RestClient.RequestBodySpec.class);
    responseSpec = mock(RestClient.ResponseSpec.class);

    when(restClientBuilder.baseUrl(anyString())).thenReturn(restClientBuilder);
    when(restClientBuilder.requestFactory(any())).thenReturn(restClientBuilder);
    when(restClientBuilder.defaultStatusHandler(any(), any())).thenReturn(restClientBuilder);
    when(restClientBuilder.build()).thenReturn(restClient);

    when(restClient.post()).thenReturn(requestBodyUriSpec);
    when(requestBodyUriSpec.contentType(any(MediaType.class))).thenReturn(requestBodySpec);
    when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
    when(requestBodySpec.retrieve()).thenReturn(responseSpec);

    discordProperties = new DiscordProperties();
    meterRegistry = new SimpleMeterRegistry();
  }

  @Test
  @DisplayName("지정된 채널의 웹훅 URL로 정상적으로 전송 요청이 진행되는지 검증")
  void sendMessage_shouldUseChannelSpecificUrl() {
    // Given
    Map<String, String> webhooks = new HashMap<>();
    webhooks.put("default", "http://default-webhook");
    webhooks.put("data", "http://data-webhook");
    discordProperties.setWebhooks(webhooks);

    discordWebhookService =
        new DiscordWebhookService(restClientBuilder, discordProperties, meterRegistry);

    DiscordWebhookPayload payload = DiscordWebhookPayload.builder().username("TestBot").build();

    // When
    discordWebhookService.sendMessage(payload, DiscordChannel.DATA);

    // Then
    verify(restClientBuilder).baseUrl("http://data-webhook");
  }

  @Test
  @DisplayName("채널 웹훅 URL이 공백이거나 누락되었을 때 default 채널 URL로 폴백 검증")
  void sendMessage_shouldFallbackToDefaultWhenChannelUrlIsEmpty() {
    // Given
    Map<String, String> webhooks = new HashMap<>();
    webhooks.put("default", "http://default-webhook");
    webhooks.put("data", ""); // Blank URL
    discordProperties.setWebhooks(webhooks);

    discordWebhookService =
        new DiscordWebhookService(restClientBuilder, discordProperties, meterRegistry);

    DiscordWebhookPayload payload = DiscordWebhookPayload.builder().username("TestBot").build();

    // When
    discordWebhookService.sendMessage(payload, DiscordChannel.DATA);

    // Then
    verify(restClientBuilder, atLeastOnce()).baseUrl("http://default-webhook");
  }
}
