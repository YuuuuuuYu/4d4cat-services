package com.services.api.common.presentation;

import com.services.core.dto.BaseResponse;
import com.services.core.exception.*;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

  private final MessageSource messageSource;
  private final MeterRegistry registry;

  private ResponseEntity<BaseResponse<Void>> createErrorResponse(
      ErrorCode errorCode, HttpStatus status, Exception e) {

    registry
        .counter("api.errors.total", "code", errorCode.getCode(), "status", status.name())
        .increment();

    String message = messageSource.getMessage(errorCode.getMessageKey(), null, Locale.getDefault());
    BaseResponse<Void> response = BaseResponse.of(status, errorCode.getCode(), message);
    log.error("Error Handled: {} - {}", errorCode.getCode(), message, e);
    return new ResponseEntity<>(response, status);
  }

  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<BaseResponse<Void>> handleBadRequestException(BadRequestException e) {
    return createErrorResponse(e.getErrorCode(), HttpStatus.BAD_REQUEST, e);
  }

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<BaseResponse<Void>> handleNotFoundException(NotFoundException e) {
    return createErrorResponse(e.getErrorCode(), HttpStatus.NOT_FOUND, e);
  }

  @ExceptionHandler(InternalServerException.class)
  public ResponseEntity<BaseResponse<Void>> handleInternalServerException(
      InternalServerException e) {
    return createErrorResponse(e.getErrorCode(), HttpStatus.INTERNAL_SERVER_ERROR, e);
  }

  @ExceptionHandler(BadGatewayException.class)
  public ResponseEntity<BaseResponse<Void>> handleBadGatewayException(BadGatewayException e) {
    return createErrorResponse(e.getErrorCode(), HttpStatus.BAD_GATEWAY, e);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<BaseResponse<Void>> handleUnhandledException(Exception e) {
    return createErrorResponse(
        ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, e);
  }
}
