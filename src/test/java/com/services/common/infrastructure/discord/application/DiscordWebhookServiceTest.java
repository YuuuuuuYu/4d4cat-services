package com.services.common.infrastructure.discord.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.services.common.infrastructure.discord.DiscordWebhookPayload;
import com.services.common.infrastructure.discord.Embed;
import com.services.common.infrastructure.discord.exception.DiscordWebhookException;
import java.io.IOException;
import java.util.Collections;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

class DiscordWebhookServiceTest {

  private static MockWebServer mockWebServer;
  private static DiscordWebhookService discordWebhookService;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeAll
  static void setUp() throws IOException {
    mockWebServer = new MockWebServer();
    mockWebServer.start();

    String baseUrl = String.format("http://localhost:%s", mockWebServer.getPort());
    discordWebhookService = new DiscordWebhookService(WebClient.builder(), baseUrl);
  }

  @AfterAll
  static void tearDown() throws IOException {
    mockWebServer.shutdown();
  }

  @Test
  @DisplayName("sendMessage는 DiscordWebhookPayload를 JSON으로 변환하여 POST 요청")
  void sendMessage_sendsPostRequest_withCorrectPayload() throws Exception {
    // Given: Discord API가 성공(204 No Content)을 반환하도록 설정
    mockWebServer.enqueue(new MockResponse().setResponseCode(204));

    Embed embed = Embed.builder().title("Test Title").description("Test Description").build();

    DiscordWebhookPayload payload =
        DiscordWebhookPayload.builder()
            .username("Test Bot")
            .content("Test Content")
            .embeds(Collections.singletonList(embed))
            .build();

    // When
    StepVerifier.create(discordWebhookService.sendMessage(payload)).verifyComplete();

    // Then
    RecordedRequest recordedRequest = mockWebServer.takeRequest(); // 요청이 올 때까지 대기
    String requestBody = recordedRequest.getBody().readUtf8();
    DiscordWebhookPayload requestedPayload =
        objectMapper.readValue(requestBody, DiscordWebhookPayload.class);

    assertThat(recordedRequest.getMethod()).isEqualTo("POST");
    assertThat(recordedRequest.getHeader("Content-Type")).isEqualTo("application/json");

    assertThat(requestedPayload.getUsername()).isEqualTo("Test Bot");
    assertThat(requestedPayload.getContent()).isEqualTo("Test Content");
    assertThat(requestedPayload.getEmbeds()).hasSize(1);
    assertThat(requestedPayload.getEmbeds().get(0).getTitle()).isEqualTo("Test Title");
    assertThat(requestedPayload.getEmbeds().get(0).getDescription()).isEqualTo("Test Description");
  }

  @Test
  @DisplayName("sendMessage는 Discord API에서 에러 응답 시 DiscordWebhookException 발생")
  void sendMessage_throwsDiscordWebhookException_onApiError() {
    // given: Discord API가 에러(500 Internal Server Error)를 반환하도록 설정
    mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("Internal Server Error"));

    Embed embed =
        Embed.builder().title("Error Test Title").description("Error Test Description").build();

    DiscordWebhookPayload payload =
        DiscordWebhookPayload.builder()
            .username("Error Bot")
            .content("Error Content")
            .embeds(Collections.singletonList(embed))
            .build();

    // When & Then
    StepVerifier.create(discordWebhookService.sendMessage(payload))
        .expectError(DiscordWebhookException.class)
        .verify();
  }
}
