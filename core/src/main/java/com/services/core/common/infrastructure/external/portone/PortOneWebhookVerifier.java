package com.services.core.common.infrastructure.external.portone;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.util.StringUtils;

/** Verifies PortOne V2 messages using the Standard Webhooks signing format. */
public final class PortOneWebhookVerifier {

  private PortOneWebhookVerifier() {}

  public static boolean verify(
      String secret, String id, String timestamp, String signature, String body) {
    if (!StringUtils.hasText(secret)
        || !StringUtils.hasText(id)
        || !StringUtils.hasText(timestamp)
        || !StringUtils.hasText(signature)
        || body == null) {
      return false;
    }
    try {
      long sentAt = Long.parseLong(timestamp);
      long now = Instant.now().getEpochSecond();
      if (sentAt < now - 300 || sentAt > now + 300) {
        return false;
      }
      String encodedKey = secret.startsWith("whsec_") ? secret.substring(6) : secret;
      byte[] key = Base64.getDecoder().decode(encodedKey);
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(key, "HmacSHA256"));
      byte[] expected =
          mac.doFinal((id + "." + timestamp + "." + body).getBytes(StandardCharsets.UTF_8));
      for (String candidate : signature.split("\\s+")) {
        if (candidate.startsWith("v1,")) {
          byte[] received = Base64.getDecoder().decode(candidate.substring(3));
          if (MessageDigest.isEqual(expected, received)) {
            return true;
          }
        }
      }
    } catch (IllegalArgumentException | GeneralSecurityException e) {
      return false;
    }
    return false;
  }
}
