package com.services.api.applydays.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PortOneWebhookRequest(String type, String timestamp, WebhookData data) {
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record WebhookData(
      String paymentId, String storeId, String transactionId, String billingKey) {}
}
