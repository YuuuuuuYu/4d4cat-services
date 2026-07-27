package com.services.api.applydays.dto;

public record PortOneWebhookRequest(String type, String timestamp, WebhookData data) {
  public record WebhookData(
      String paymentId, String storeId, String transactionId, String billingKey) {}
}
