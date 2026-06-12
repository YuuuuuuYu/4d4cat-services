package com.services.api.common.presentation;

import com.services.core.common.dto.BaseResponse;
import com.services.core.common.exception.BadGatewayException;
import com.services.core.common.exception.BadRequestException;
import com.services.core.common.exception.ErrorCode;
import com.services.core.common.exception.ForbiddenException;
import com.services.core.common.exception.InternalServerException;
import com.services.core.common.exception.NotFoundException;
import com.services.core.common.exception.UnauthorizedException;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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
    if (status.is4xxClientError()) {
      log.warn(
          "Error Handled (Client): {} - {} (Exception: {})",
          errorCode.getCode(),
          message,
          e.getClass().getSimpleName());
    } else {
      log.error("Error Handled (Server): {} - {}", errorCode.getCode(), message, e);
    }
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

  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<BaseResponse<Void>> handleUnauthorizedException(UnauthorizedException e) {
    return createErrorResponse(e.getErrorCode(), HttpStatus.UNAUTHORIZED, e);
  }

  @ExceptionHandler(ForbiddenException.class)
  public ResponseEntity<BaseResponse<Void>> handleForbiddenException(ForbiddenException e) {
    return createErrorResponse(e.getErrorCode(), HttpStatus.FORBIDDEN, e);
  }

  @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
  public ResponseEntity<BaseResponse<Void>> handleAccessDeniedException(Exception e) {
    return createErrorResponse(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN, e);
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

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<BaseResponse<Void>> handleHttpRequestMethodNotSupportedException(
      HttpRequestMethodNotSupportedException e) {
    return createErrorResponse(ErrorCode.INVALID_REQUEST, HttpStatus.METHOD_NOT_ALLOWED, e);
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<BaseResponse<Void>> handleHttpMediaTypeNotSupportedException(
      HttpMediaTypeNotSupportedException e) {
    return createErrorResponse(ErrorCode.INVALID_REQUEST, HttpStatus.UNSUPPORTED_MEDIA_TYPE, e);
  }

  @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
  public ResponseEntity<Void> handleHttpMediaTypeNotAcceptableException(
      HttpMediaTypeNotAcceptableException e) {
    log.warn("Error Handled (Client): HttpMediaTypeNotAcceptableException - {}", e.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<BaseResponse<Void>> handleNoResourceFoundException(
      NoResourceFoundException e) {
    return createErrorResponse(ErrorCode.INVALID_REQUEST, HttpStatus.NOT_FOUND, e);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<BaseResponse<Void>> handleUnhandledException(Exception e) {
    return createErrorResponse(
        ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, e);
  }
}
