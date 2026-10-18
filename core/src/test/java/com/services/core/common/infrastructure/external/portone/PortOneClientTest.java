package com.services.core.common.infrastructure.external.portone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class PortOneClientTest {

  private MockRestServiceServer server;
  private PortOneClient client;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder();
    server = MockRestServiceServer.bindTo(builder).build();
    client = new PortOneClient("https://api.portone.io", "test-api-secret", builder);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"mock", " ", "http://api.portone.io", "https:/invalid"})
  void invalidConfigurationCannotEnableMockPayments(String url) {
    assertThatThrownBy(() -> new PortOneClient(url, "test-api-secret"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void missingSecretFailsClosed() {
    assertThatThrownBy(() -> new PortOneClient("https://api.portone.io", ""))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void billingPaymentPostsJsonThenVerifiesActualPayment() {
    server
        .expect(requestTo("https://api.portone.io/payments/sub_live/billing-key"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "PortOne test-api-secret"))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(
            content()
                .json(
                    """
            {"billingKey":"billing_live","orderName":"Premium",
             "amount":{"total":16500},"currency":"KRW"}
            """))
        .andRespond(
            withSuccess("{\"payment\":{\"pgTxId\":\"tx_live\"}}", MediaType.APPLICATION_JSON));
    server
        .expect(requestTo("https://api.portone.io/payments/sub_live"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withSuccess(
                """
            {"id":"sub_live","status":"PAID","amount":{"total":16500,"vat":1500},
             "currency":"KRW","pgTxId":"tx_live","receiptUrl":"https://receipt.example/live"}
            """,
                MediaType.APPLICATION_JSON));

    var result = client.payWithBillingKey("billing_live", "sub_live", 16500, "Premium");

    assertThat(result.status()).isEqualTo("PAID");
    assertThat(result.amount().total()).isEqualTo(16500);
    assertThat(result.currency()).isEqualTo("KRW");
    assertThat(result.pgTxId()).isEqualTo("tx_live");
    server.verify();
  }

  @Test
  void issuanceReadsBillingKeyFromNestedBillingKeyInfo() {
    server
        .expect(requestTo("https://api.portone.io/billing-keys"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(
            content()
                .json(
                    """
            {"channelKey":"channel_live","customer":{"id":"customer_live"},
             "method":{"card":{"credential":{"number":"1111111111111111",
              "expiryMonth":"12","expiryYear":"29",
              "birthOrBusinessRegistrationNumber":"900101","passwordTwoDigits":"00"}}}}
            """))
        .andRespond(
            withSuccess(
                """
            {"billingKeyInfo":{"status":"ISSUED","billingKey":"billing_live"}}
            """,
                MediaType.APPLICATION_JSON));

    var result =
        client.issueBillingKey(
            "customer_live", "channel_live", "1111111111111111", "12", "29", "900101", "00");

    assertThat(result.billingKey()).isEqualTo("billing_live");
    server.verify();
  }

  @Test
  void upstreamErrorDoesNotExposeResponseBodyOrCredentials() {
    server
        .expect(requestTo("https://api.portone.io/payments/payment_fail"))
        .andRespond(withServerError().body("private-provider-details test-api-secret"));

    assertThatThrownBy(() -> client.verifyPayment("payment_fail"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Failed to verify payment with PortOne V2")
        .hasNoCause();
    server.verify();
  }

  @Test
  void deleteAlwaysCallsPortOne() {
    server
        .expect(requestTo("https://api.portone.io/billing-keys/billing_live"))
        .andExpect(method(HttpMethod.DELETE))
        .andExpect(header("Authorization", "PortOne test-api-secret"))
        .andRespond(withSuccess());

    client.deleteBillingKey("billing_live");

    server.verify();
  }
}
