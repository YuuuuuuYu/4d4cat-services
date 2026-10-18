package com.services.core.common.infrastructure.external.portone;

import java.net.URI;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class PortOneClient {

  private final RestClient restClient;

  @Autowired
  public PortOneClient(
      @Value("${app.portone.api-url}") String apiUrl,
      @Value("${app.portone.api-secret}") String apiSecret) {
    this(apiUrl, apiSecret, requestBuilder());
  }

  PortOneClient(String apiUrl, String apiSecret, RestClient.Builder builder) {
    if (!StringUtils.hasText(apiUrl)
        || !"https".equalsIgnoreCase(URI.create(apiUrl).getScheme())
        || !StringUtils.hasText(URI.create(apiUrl).getHost())) {
      throw new IllegalArgumentException("PortOne API URL must be an HTTPS URL");
    }
    if (!StringUtils.hasText(apiSecret)) {
      throw new IllegalArgumentException("PortOne V2 API secret is required");
    }
    this.restClient =
        builder.baseUrl(apiUrl).defaultHeader("Authorization", "PortOne " + apiSecret).build();
  }

  private static RestClient.Builder requestBuilder() {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(5000);
    factory.setReadTimeout(5000);
    return RestClient.builder().requestFactory(factory);
  }

  public record PortOnePaymentResponse(
      String id,
      String status,
      Amount amount,
      String billingKey,
      FailedInfo failed,
      String receiptUrl,
      String pgTxId,
      String currency) {}

  public record Amount(long total) {}

  public record FailedInfo(String reason) {}

  public PortOnePaymentResponse verifyPayment(String paymentId) {

    try {
      PortOnePaymentResponse res =
          restClient
              .get()
              .uri("/payments/{paymentId}", paymentId)
              .retrieve()
              .body(PortOnePaymentResponse.class);

      if (res == null) {
        throw new IllegalStateException("PortOne payment verification returned null response");
      }
      return res;
    } catch (RestClientException e) {
      log.warn("Error verifying payment via PortOne V2");
      throw new IllegalStateException("Failed to verify payment with PortOne V2");
    }
  }

  public PortOnePaymentResponse payWithBillingKey(
      String billingKey, String paymentId, long amount, String orderName) {
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
      restClient
          .post()
          .uri("/payments/{paymentId}/billing-key", paymentId)
          .contentType(MediaType.APPLICATION_JSON)
          .body(body)
          .retrieve()
          .toBodilessEntity();

      // Step 2: 결제 승인 완료 후, GET 조회 API를 통해 status/amount 등 상세 정보를 획득
      log.info("Fetching full payment details via GET /payments/{}", paymentId);
      return verifyPayment(paymentId);

    } catch (RestClientException e) {
      log.warn("Error requesting billing-key payment via PortOne V2");
      throw new IllegalStateException("Failed to request billing-key payment with PortOne V2");
    }
  }

  public record PortOneBillingKeyResponse(String billingKey) {}

  private record IssueBillingKeyResponse(PortOneBillingKeyResponse billingKeyInfo) {}

  public PortOneBillingKeyResponse issueBillingKey(
      String customerId,
      String channelKey,
      String cardNumber,
      String expiryMonth,
      String expiryYear,
      String birthOrBusinessRegistrationNumber,
      String passwordTwoDigits) {
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

      IssueBillingKeyResponse res =
          restClient
              .post()
              .uri("/billing-keys")
              .contentType(MediaType.APPLICATION_JSON)
              .body(body)
              .retrieve()
              .body(IssueBillingKeyResponse.class);

      if (res == null
          || res.billingKeyInfo() == null
          || !StringUtils.hasText(res.billingKeyInfo().billingKey())) {
        throw new IllegalStateException("PortOne billing key issuance returned empty response");
      }
      return res.billingKeyInfo();
    } catch (RestClientException e) {
      log.warn("Error issuing billing key via PortOne V2 KCP pg-api");
      throw new IllegalStateException("Failed to issue billing key");
    }
  }

  public void deleteBillingKey(String billingKey) {

    try {
      restClient
          .delete()
          .uri("/billing-keys/{billingKey}", billingKey)
          .retrieve()
          .toBodilessEntity();
    } catch (RestClientException e) {
      log.warn("Error deleting billing key via PortOne V2");
      throw new IllegalStateException("Failed to delete billing key");
    }
  }
}
