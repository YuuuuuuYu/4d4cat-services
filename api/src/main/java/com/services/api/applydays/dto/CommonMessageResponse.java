package com.services.api.applydays.dto;

import lombok.Builder;

@Builder
public record CommonMessageResponse(String message) {
  public static CommonMessageResponse of(String message) {
    return new CommonMessageResponse(message);
  }
}
