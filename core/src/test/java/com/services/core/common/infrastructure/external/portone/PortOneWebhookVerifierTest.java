package com.services.core.common.infrastructure.external.portone;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class PortOneWebhookVerifierTest {

  private static final String SECRET = "whsec_dGVzdC1zZWNyZXQ=";
  private static final String BODY = "{\"type\":\"Transaction.Paid\"}";

  @Test
  void acceptsStandardWebhooksSignatureAndRotatedSignatures() throws Exception {
    String timestamp = Long.toString(Instant.now().getEpochSecond());
    String signature = sign(timestamp);
    assertThat(PortOneWebhookVerifier.verify(SECRET, "msg_test", timestamp, signature, BODY))
        .isTrue();
    assertThat(
            PortOneWebhookVerifier.verify(
                SECRET, "msg_test", timestamp, "v1,d3Jvbmctc2lnbmF0dXJl " + signature, BODY))
        .isTrue();
  }

  @Test
  void rejectsTamperingAndMissingConfiguration() throws Exception {
    String timestamp = Long.toString(Instant.now().getEpochSecond());
    String signature = sign(timestamp);
    assertThat(PortOneWebhookVerifier.verify(SECRET, "msg_test", timestamp, signature, BODY + " "))
        .isFalse();
    assertThat(PortOneWebhookVerifier.verify(SECRET, "msg_other", timestamp, signature, BODY))
        .isFalse();
    assertThat(PortOneWebhookVerifier.verify("", "msg_test", timestamp, signature, BODY)).isFalse();
    assertThat(PortOneWebhookVerifier.verify(SECRET, "msg_test", timestamp, SECRET, BODY))
        .isFalse();
    assertThat(PortOneWebhookVerifier.verify(SECRET, "msg_test", timestamp, "v1,???", BODY))
        .isFalse();
  }

  @Test
  void rejectsExpiredAndFutureMessages() throws Exception {
    for (long offset : new long[] {-600, 600}) {
      String timestamp = Long.toString(Instant.now().getEpochSecond() + offset);
      assertThat(
              PortOneWebhookVerifier.verify(SECRET, "msg_test", timestamp, sign(timestamp), BODY))
          .isFalse();
    }
  }

  private String sign(String timestamp) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec("test-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    return "v1,"
        + Base64.getEncoder()
            .encodeToString(
                mac.doFinal(
                    ("msg_test." + timestamp + "." + BODY).getBytes(StandardCharsets.UTF_8)));
  }
}
