package com.services.core.common.notification.discord;

import com.services.core.common.exception.BadGatewayException;
import com.services.core.common.exception.ErrorCode;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
public class DiscordWebhookService {

  private final MeterRegistry registry;
  private final Map<String, RestClient> restClients;

  public DiscordWebhookService(
      RestClient.Builder restClientBuilder,
      DiscordProperties discordProperties,
      MeterRegistry registry) {
    this.registry = registry;

    Map<String, String> webhooks =
        discordProperties.getWebhooks() != null
            ? discordProperties.getWebhooks()
            : Collections.emptyMap();

    String defaultUrl = webhooks.get("default");
    if (defaultUrl == null || defaultUrl.isBlank()) {
      log.warn("Default Discord webhook URL is not configured or blank.");
      defaultUrl = "";
    }

    Map<String, RestClient> tempClients = new HashMap<>();

    for (DiscordChannel channel : DiscordChannel.values()) {
      String channelName = channel.getValue();
      String url = webhooks.get(channelName);

      if (url == null || url.isBlank()) {
        log.info(
            "Webhook URL for channel '{}' is not configured. Falling back to 'default' channel URL.",
            channelName);
        url = defaultUrl;
      }

      final String finalUrl = url;
      RestClient client =
          restClientBuilder
              .baseUrl(finalUrl)
              .defaultStatusHandler(
                  HttpStatusCode::isError,
                  (request, response) -> {
                    log.error(
                        "Failed to send Discord webhook to channel {}. Status: {}",
                        channelName,
                        response.getStatusCode());
                    throw new BadGatewayException(ErrorCode.DISCORD_WEBHOOK_FAILED);
                  })
              .build();

      tempClients.put(channelName, client);
    }

    this.restClients = Collections.unmodifiableMap(tempClients);
  }

  public void sendMessage(DiscordWebhookPayload payload) {
    sendMessage(payload, DiscordChannel.DEFAULT);
  }

  public void sendMessage(DiscordWebhookPayload payload, DiscordChannel channel) {
    String channelName = channel.getValue();
    try {
      RestClient client = restClients.get(channelName);
      if (client == null) {
        log.error("Unconfigured channel: {}", channelName);
        return;
      }
      client
          .post()
          .contentType(MediaType.APPLICATION_JSON)
          .body(payload)
          .retrieve()
          .toBodilessEntity();
      registry
          .counter("discord.webhook.sent", "status", "success", "channel", channelName)
          .increment();
    } catch (Exception e) {
      registry
          .counter("discord.webhook.sent", "status", "failure", "channel", channelName)
          .increment();
      throw e;
    }
  }

  public void sendMessageAsync(DiscordWebhookPayload payload) {
    sendMessageAsync(payload, DiscordChannel.DEFAULT);
  }

  public void sendMessageAsync(DiscordWebhookPayload payload, DiscordChannel channel) {
    Thread.startVirtualThread(
        () -> {
          try {
            sendMessage(payload, channel);
          } catch (Exception e) {
            log.error(
                "Async Discord webhook failed for channel {}: {}",
                channel.getValue(),
                e.getMessage());
          }
        });
  }
}
