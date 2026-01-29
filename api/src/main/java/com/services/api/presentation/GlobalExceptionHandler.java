package com.services.api.presentation;

import com.services.core.dto.BaseResponse;
import com.services.core.exception.BadGatewayException;
import com.services.core.exception.BadRequestException;
import com.services.core.exception.CustomException;
import com.services.core.exception.InternalServerException;
import com.services.core.exception.NotFoundException;
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

  private ResponseEntity<BaseResponse<Void>> createErrorResponse(
      CustomException e, HttpStatus status) {
    String message =
        messageSource.getMessage(e.getErrorCode().getMessageKey(), null, Locale.getDefault());
    BaseResponse<Void> response = BaseResponse.of(status, e.getErrorCode().getCode(), message);
    log.error("Error Handled: {} - {}", e.getErrorCode().getCode(), message, (Exception) e);
    return new ResponseEntity<>(response, status);
  }

  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<BaseResponse<Void>> handleBadRequestException(BadRequestException e) {
    return createErrorResponse(e, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<BaseResponse<Void>> handleNotFoundException(NotFoundException e) {
    return createErrorResponse(e, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(InternalServerException.class)
  public ResponseEntity<BaseResponse<Void>> handleInternalServerException(
      InternalServerException e) {
    return createErrorResponse(e, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(BadGatewayException.class)
  public ResponseEntity<BaseResponse<Void>> handleBadGatewayException(BadGatewayException e) {
    return createErrorResponse(e, HttpStatus.BAD_GATEWAY);
  }
}
