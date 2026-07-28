package com.services.core.common.infrastructure.external.sendpulse;

import com.services.core.common.infrastructure.external.sendpulse.dto.SendPulseEmailRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.notification.sendpulse.api-key")
public class SendPulseEmailClient {

  private final RestClient restClient;

  public SendPulseEmailClient() {
    this(createDefaultRestClient());
  }

  public SendPulseEmailClient(RestClient restClient) {
    this.restClient = restClient;
  }

  private static RestClient createDefaultRestClient() {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(5000);
    factory.setReadTimeout(5000);
    return RestClient.builder().requestFactory(factory).build();
  }

  @Value("${app.notification.sendpulse.api-key}")
  private String apiKey;

  @Value("${app.notification.sendpulse.api-url}")
  private String apiUrl;

  public void sendEmail(SendPulseEmailRequest request) {
    try {
      restClient
          .post()
          .uri(apiUrl)
          .header("Authorization", "Bearer " + apiKey)
          .body(request)
          .retrieve()
          .toBodilessEntity();
      log.info("Successfully sent email via SendPulse to {}", request.email().to().get(0).email());
    } catch (Exception e) {
      log.error("Failed to send email via SendPulse", e);
      throw e;
    }
  }
}
