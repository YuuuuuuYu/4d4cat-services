package com.services.core.dto;

import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;

public record BaseResponse<T>(int status, T data, ErrorResponse error, LocalDateTime timestamp) {

  static class ErrorResponse {
    private String code;
    private String message;

    public ErrorResponse(String code, String message) {
      this.code = code;
      this.message = message;
    }

    public String getCode() {
      return code;
    }

    public String getMessage() {
      return message;
    }
  }

  public static <T> BaseResponse<T> of(HttpStatus status, T data) {
    return new BaseResponse<>(status.value(), data, null, LocalDateTime.now());
  }

  public static <T> BaseResponse<T> of(HttpStatus status, String code, String message) {
    return new BaseResponse<>(
        status.value(), null, new ErrorResponse(code, message), LocalDateTime.now());
  }
}
