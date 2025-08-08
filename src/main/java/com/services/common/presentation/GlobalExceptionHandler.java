package com.services.common.presentation;

import com.services.common.application.exception.BadGatewayException;
import com.services.common.application.exception.BadRequestException;
import com.services.common.application.exception.InternalServerException;
import com.services.common.application.exception.NotFoundException;
import com.services.common.presentation.dto.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<BaseResponse<Void>> handleBadRequestException(BadRequestException e) {
        BaseResponse<Void> response = BaseResponse.of(HttpStatus.BAD_REQUEST, e.getErrorCode());
        log.error("Bad Request: {}", e);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<BaseResponse<Void>> handleNotFoundException(NotFoundException e) {
        BaseResponse<Void> response = BaseResponse.of(HttpStatus.NOT_FOUND, e.getErrorCode());
        log.error("Not Found: {}", e);
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InternalServerException.class)
    public ResponseEntity<BaseResponse<Void>> handleInternalServerException(InternalServerException e) {
        BaseResponse<Void> response = BaseResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, e.getErrorCode());
        log.error("Internal Server Error: {}", e);
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(BadGatewayException.class)
    public ResponseEntity<BaseResponse<Void>> handleBadGatewayException(BadGatewayException e) {
        BaseResponse<Void> response = BaseResponse.of(HttpStatus.BAD_GATEWAY, e.getErrorCode());
        log.error("Bad Gateway: {}", e);
        return new ResponseEntity<>(response, HttpStatus.BAD_GATEWAY);
    }
}