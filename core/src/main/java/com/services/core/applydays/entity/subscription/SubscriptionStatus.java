package com.services.core.applydays.entity.subscription;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SubscriptionStatus {
  ACTIVE("활성"),
  CANCELED("취소"),
  EXPIRED("만료"),
  PAYMENT_FAILED("결제실패");

  private final String description;
}
