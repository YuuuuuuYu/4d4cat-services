package com.services.core.applydays.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum HiringStep {
  DOCUMENT("서류 전형"),
  CODING("코딩 테스트"),
  ASSIGNMENT("사전 과제"),
  TECH("직무 면접"),
  CULTURE("컬처/인성 면접"),
  OFFER("처우 협의/오퍼");

  private final String description;
}
