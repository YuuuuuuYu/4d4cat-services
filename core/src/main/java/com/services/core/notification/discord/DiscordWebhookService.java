package com.services.core.notification.discord;

import com.services.core.exception.BadGatewayException;
import com.services.core.exception.ErrorCode;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
public class DiscordWebhookService {

  private final RestClient restClient;
  private final MeterRegistry registry;

  public DiscordWebhookService(
      RestClient.Builder restClientBuilder,
      @Value("${discord.webhook.url}") String webhookUrl,
      MeterRegistry registry) {
    this.registry = registry;
    this.restClient =
        restClientBuilder
            .baseUrl(webhookUrl)
            .defaultStatusHandler(
                HttpStatusCode::isError,
                (request, response) -> {
                  log.error("Failed to send Discord webhook. Status: {}", response.getStatusCode());
                  throw new BadGatewayException(ErrorCode.DISCORD_WEBHOOK_FAILED);
                })
            .build();
  }

  public void sendMessage(DiscordWebhookPayload payload) {
    try {
      restClient
          .post()
          .contentType(MediaType.APPLICATION_JSON)
          .body(payload)
          .retrieve()
          .toBodilessEntity();
      registry.counter("discord.webhook.sent", "status", "success").increment();
    } catch (Exception e) {
      registry.counter("discord.webhook.sent", "status", "failure").increment();
      throw e;
    }
  }

  public void sendMessageAsync(DiscordWebhookPayload payload) {
    Thread.startVirtualThread(
        () -> {
          try {
            sendMessage(payload);
          } catch (Exception e) {
            log.error("Async Discord webhook failed: {}", e.getMessage());
          }
        });
  }
}
