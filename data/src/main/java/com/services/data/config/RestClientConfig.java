package com.services.data.config;

import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

@Configuration
@Slf4j
public class RestClientConfig {

  @Bean
  public RestClient restClient(RestClient.Builder builder) {
    return builder
        .defaultStatusHandler(
            HttpStatusCode::isError,
            (request, response) -> {
              String errorBody =
                  new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
              int statusCode = response.getStatusCode().value();
              log.warn(
                  "API call failed - URL: {}, Status: {}, Body: {}",
                  request.getURI(),
                  statusCode,
                  errorBody);
            })
        .build();
  }
}
