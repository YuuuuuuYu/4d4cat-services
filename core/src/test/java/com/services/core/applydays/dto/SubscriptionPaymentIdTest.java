package com.services.core.applydays.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class SubscriptionPaymentIdTest {

  @Test
  void compactIdPreservesMemberAndFitsKcpLimit() {
    UUID memberId = UUID.randomUUID();
    String id = SubscriptionPaymentId.create(memberId);

    assertThat(id).hasSizeLessThanOrEqualTo(40).matches("[a-zA-Z0-9_-]+");
    assertThat(SubscriptionPaymentId.memberId(id)).isEqualTo(memberId);
    assertThat(SubscriptionPaymentId.create(memberId)).isNotEqualTo(id);
  }

  @Test
  void oldPaymentIdsRemainReadable() {
    UUID memberId = UUID.randomUUID();
    assertThat(SubscriptionPaymentId.memberId("sub_" + memberId + "_123456")).isEqualTo(memberId);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(
      strings = {"invalid", "sub_", "sub_invalid_123", "sub_!!!!!!!!!!!!!!!!!!!!!!_123456789012"})
  void invalidIdCannotResolveMember(String id) {
    assertThat(SubscriptionPaymentId.memberId(id)).isNull();
  }
}
