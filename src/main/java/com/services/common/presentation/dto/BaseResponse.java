package com.services.common.presentation.dto;

import com.services.common.application.exception.ErrorCode;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

public record BaseResponse<T>(
        int status,
        T data,
        ErrorResponse error,
        LocalDateTime timestamp
    ) {

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

    public static <T> BaseResponse<T> of(HttpStatus status, ErrorCode errorCode) {
        ErrorResponse errorResponse = new ErrorResponse(errorCode.getCode(), errorCode.getMessage());
        return new BaseResponse<>(status.value(), null, errorResponse, LocalDateTime.now());
    }
}
