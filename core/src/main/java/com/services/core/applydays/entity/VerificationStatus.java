package com.services.core.applydays.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum VerificationStatus {
  PENDING("대기"),
  APPROVED("승인"),
  REJECTED("거절");

  private final String description;
}
