package com.services.common.infrastructure.discord.application;

import com.services.common.application.exception.ErrorCode;
import com.services.common.infrastructure.discord.DiscordWebhookPayload;
import com.services.common.infrastructure.discord.exception.DiscordWebhookException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class DiscordWebhookService {

  private final WebClient webClient;

  public DiscordWebhookService(
      WebClient.Builder webClientBuilder, @Value("${discord.webhook.url}") String webhookUrl) {
    this.webClient = webClientBuilder.baseUrl(webhookUrl).build();
  }

  public Mono<Void> sendMessage(DiscordWebhookPayload payload) {
    return webClient
        .post()
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(payload)
        .retrieve()
        .onStatus(
            status -> status.isError(),
            response ->
                response
                    .bodyToMono(String.class)
                    .flatMap(
                        body -> {
                          log.error(
                              "Failed to send Discord webhook. Status: {}, Response: {}",
                              response.statusCode(),
                              body);
                          return Mono.error(
                              new DiscordWebhookException(ErrorCode.DISCORD_WEBHOOK_FAILED));
                        }))
        .bodyToMono(Void.class);
  }
}
