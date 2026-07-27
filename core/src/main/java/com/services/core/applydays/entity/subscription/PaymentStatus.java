package com.services.core.applydays.entity.subscription;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentStatus {
  PENDING("대기"),
  SUCCESS("성공"),
  FAILED("실패");

  private final String description;
}
