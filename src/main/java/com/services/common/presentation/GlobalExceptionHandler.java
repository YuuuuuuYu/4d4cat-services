package com.services.common.presentation;

import com.services.common.application.exception.BadGatewayException;
import com.services.common.application.exception.BadRequestException;
import com.services.common.application.exception.CustomException;
import com.services.common.application.exception.InternalServerException;
import com.services.common.application.exception.NotFoundException;
import com.services.common.presentation.dto.BaseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Locale;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    private ResponseEntity<BaseResponse<Void>> createErrorResponse(CustomException e, HttpStatus status) {
        String message = messageSource.getMessage(e.getErrorCode().getMessageKey(), null, Locale.getDefault());
        BaseResponse<Void> response = BaseResponse.of(status, e.getErrorCode().getCode(), message);
        log.error("Error Handled: {} - {}", e.getErrorCode().getCode(), message, e);
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
    public ResponseEntity<BaseResponse<Void>> handleInternalServerException(InternalServerException e) {
        return createErrorResponse(e, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(BadGatewayException.class)
    public ResponseEntity<BaseResponse<Void>> handleBadGatewayException(BadGatewayException e) {
        return createErrorResponse(e, HttpStatus.BAD_GATEWAY);
    }
}
