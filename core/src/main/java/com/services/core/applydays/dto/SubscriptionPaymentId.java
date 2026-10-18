package com.services.core.applydays.dto;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.UUID;

/** KCP's 40-character limit, with member information retained for webhook reconciliation. */
public final class SubscriptionPaymentId {

  private SubscriptionPaymentId() {}

  public static String create(UUID memberId) {
    byte[] bytes =
        ByteBuffer.allocate(16)
            .putLong(memberId.getMostSignificantBits())
            .putLong(memberId.getLeastSignificantBits())
            .array();
    return "sub_"
        + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        + "_"
        + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
  }

  public static UUID memberId(String paymentId) {
    if (paymentId == null || !paymentId.startsWith("sub_")) {
      return null;
    }
    try {
      if (paymentId.length() == 39 && paymentId.charAt(26) == '_') {
        ByteBuffer bytes =
            ByteBuffer.wrap(Base64.getUrlDecoder().decode(paymentId.substring(4, 26)));
        if (bytes.remaining() != 16) {
          return null;
        }
        return new UUID(bytes.getLong(), bytes.getLong());
      }
      // Retain support for payment IDs issued before the live KCP integration.
      if (paymentId.length() > 40 && paymentId.charAt(40) == '_') {
        return UUID.fromString(paymentId.substring(4, 40));
      }
    } catch (IllegalArgumentException e) {
      return null;
    }
    return null;
  }
}
