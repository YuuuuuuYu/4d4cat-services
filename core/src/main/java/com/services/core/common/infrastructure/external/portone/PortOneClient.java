package com.services.core.common.infrastructure.external.portone;

import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class PortOneClient {

  private final RestClient restClient;
  private final String apiUrl;
  private final String apiSecret;
  private final long mockAmount;

  public PortOneClient(
      @Value("${app.portone.api-url}") String apiUrl,
      @Value("${app.portone.api-secret}") String apiSecret,
      @Value("${app.portone.mock-amount}") long mockAmount) {
    this.apiUrl = apiUrl;
    this.apiSecret = apiSecret;
    this.mockAmount = mockAmount;

    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(5000);
    factory.setReadTimeout(5000);

    this.restClient = RestClient.builder().requestFactory(factory).build();
  }

  public record PortOnePaymentResponse(
      String id,
      String status,
      Amount amount,
      String billingKey,
      FailedInfo failed,
      String receiptUrl,
      String pgTxId) {}

  public record Amount(long total, String currency) {}

  public record FailedInfo(String reason) {}

  public PortOnePaymentResponse verifyPayment(String paymentId) {
    log.info("Verifying payment with PortOne V2: paymentId={}, apiUrl={}", paymentId, apiUrl);
    if ("mock".equalsIgnoreCase(apiUrl) || apiUrl.isBlank()) {
      log.info("Running PortOneClient V2 in mock mode");
      if (paymentId.contains("fail")) {
        return new PortOnePaymentResponse(
            paymentId,
            "FAILED",
            new Amount(mockAmount, "KRW"),
            null,
            new FailedInfo("Mock limit exceeded"),
            null,
            null);
      }
      String billingKey = "billing_" + paymentId;
      return new PortOnePaymentResponse(
          paymentId,
          "PAID",
          new Amount(mockAmount, "KRW"),
          billingKey,
          null,
          "https://receipt.portone.io/v2/mock_receipt",
          "mock_tx_" + UUID.randomUUID().toString().substring(0, 8));
    }

    try {
      PortOnePaymentResponse res =
          restClient
              .get()
              .uri(apiUrl + "/payments/" + paymentId)
              .header("Authorization", "PortOne " + apiSecret)
              .retrieve()
              .body(PortOnePaymentResponse.class);

      if (res == null) {
        throw new RuntimeException("PortOne payment verification returned null response");
      }
      return res;
    } catch (Exception e) {
      log.error("Error verifying payment via PortOne V2", e);
      throw new RuntimeException("Failed to verify payment with PortOne V2");
    }
  }

  public record PortOneBillingKeyPaymentResponse(PortOnePaymentResponse payment) {}

  public PortOnePaymentResponse payWithBillingKey(
      String billingKey, String paymentId, long amount, String orderName) {
    log.info(
        "Requesting billing key payment with PortOne V2: billingKey={}, paymentId={}, amount={}, orderName={}",
        billingKey,
        paymentId,
        amount,
        orderName);
    if ("mock".equalsIgnoreCase(apiUrl) || apiUrl.isBlank()) {
      log.info("Running PortOneClient V2 in mock mode");
      if (billingKey.contains("fail")) {
        return new PortOnePaymentResponse(
            paymentId,
            "FAILED",
            new Amount(amount, "KRW"),
            billingKey,
            new FailedInfo("Mock card validation failed"),
            null,
            null);
      }
      return new PortOnePaymentResponse(
          paymentId,
          "PAID",
          new Amount(amount, "KRW"),
          billingKey,
          null,
          "https://receipt.portone.io/v2/mock_receipt",
          "mock_tx_" + UUID.randomUUID().toString().substring(0, 8));
    }

    try {
      Map<String, Object> body =
          Map.of(
              "billingKey",
              billingKey,
              "orderName",
              orderName,
              "amount",
              Map.of("total", amount),
              "currency",
              "KRW");

      // Step 1: 빌링키 결제 승인 요청 (응답에는 pgTxId, paidAt 등 최소 정보만 포함됨)
      String rawJson =
          restClient
              .post()
              .uri(apiUrl + "/payments/" + paymentId + "/billing-key")
              .header("Authorization", "PortOne " + apiSecret)
              .body(body)
              .retrieve()
              .body(String.class);

      log.info("PortOne billing-key payment raw response: {}", rawJson);

      // Step 2: 결제 승인 완료 후, GET 조회 API를 통해 status/amount 등 상세 정보를 획득
      log.info("Fetching full payment details via GET /payments/{}", paymentId);
      return verifyPayment(paymentId);

    } catch (Exception e) {
      log.error("Error requesting billing-key payment via PortOne V2", e);
      throw new RuntimeException("Failed to request billing-key payment with PortOne V2");
    }
  }

  public record PortOneBillingKeyResponse(String billingKey) {}

  public PortOneBillingKeyResponse issueBillingKey(
      String customerId,
      String channelKey,
      String cardNumber,
      String expiryMonth,
      String expiryYear,
      String birthOrBusinessRegistrationNumber,
      String passwordTwoDigits) {
    log.info(
        "Issuing billing key with PortOne V2 KCP pg-api: customerId={}, channelKey={}",
        customerId,
        channelKey);
    if ("mock".equalsIgnoreCase(apiUrl) || apiUrl.isBlank()) {
      log.info("Running PortOneClient V2 in mock mode");
      if (cardNumber.contains("fail")) {
        throw new RuntimeException("Mock card validation failed");
      }
      return new PortOneBillingKeyResponse("mock_billing_key_" + UUID.randomUUID());
    }

    try {
      Map<String, Object> credential =
          Map.of(
              "number", cardNumber,
              "expiryMonth", expiryMonth,
              "expiryYear", expiryYear,
              "birthOrBusinessRegistrationNumber", birthOrBusinessRegistrationNumber,
              "passwordTwoDigits", passwordTwoDigits);

      Map<String, Object> card = Map.of("credential", credential);

      Map<String, Object> method = Map.of("card", card);

      Map<String, Object> body =
          Map.of(
              "channelKey", channelKey,
              "customer", Map.of("id", customerId),
              "method", method);

      PortOneBillingKeyResponse res =
          restClient
              .post()
              .uri(apiUrl + "/billing-keys")
              .header("Authorization", "PortOne " + apiSecret)
              .body(body)
              .retrieve()
              .body(PortOneBillingKeyResponse.class);

      if (res == null) {
        throw new RuntimeException("PortOne billing key issuance returned null response");
      }
      return res;
    } catch (Exception e) {
      log.error("Error issuing billing key via PortOne V2 KCP pg-api", e);
      throw new RuntimeException("Failed to issue billing key");
    }
  }

  public void deleteBillingKey(String billingKey) {
    log.info("Deleting billing key via PortOne V2: billingKey={}", billingKey);
    if ("mock".equalsIgnoreCase(apiUrl) || apiUrl.isBlank()) {
      log.info("Running PortOneClient V2 in mock mode");
      if (billingKey != null && billingKey.contains("fail_delete")) {
        throw new RuntimeException("Mock billing key deletion failed");
      }
      return;
    }

    try {
      restClient
          .delete()
          .uri(apiUrl + "/billing-keys/" + billingKey)
          .header("Authorization", "PortOne " + apiSecret)
          .retrieve()
          .toBodilessEntity();
    } catch (Exception e) {
      log.error("Error deleting billing key via PortOne V2", e);
      throw new RuntimeException("Failed to delete billing key with PortOne V2");
    }
  }
}
