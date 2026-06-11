package com.services.core.common.persistence.entity.member;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Role {
  GUEST("ROLE_GUEST", "비회원"),
  USER("ROLE_USER", "일반"),
  REVIEWER("ROLE_REVIEWER", "리뷰작성자"),
  SUBSCRIBER("ROLE_SUBSCRIBER", "구독자"),
  ADMIN("ROLE_ADMIN", "관리자");

  private final String key;
  private final String title;
}
