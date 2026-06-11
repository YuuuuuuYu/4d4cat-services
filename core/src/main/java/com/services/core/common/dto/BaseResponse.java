package com.services.core.common.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BaseResponse<T> implements Serializable {

  private int status;
  private T data;
  private ErrorResponse error;
  private LocalDateTime timestamp;

  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ErrorResponse implements Serializable {
    private String code;
    private String message;
  }

  public static <T> BaseResponse<T> of(HttpStatus status, T data) {
    return new BaseResponse<>(status.value(), data, null, LocalDateTime.now());
  }

  public static <T> BaseResponse<T> of(HttpStatus status, String code, String message) {
    return new BaseResponse<>(
        status.value(), null, new ErrorResponse(code, message), LocalDateTime.now());
  }
}
