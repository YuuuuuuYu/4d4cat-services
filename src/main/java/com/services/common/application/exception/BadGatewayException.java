package com.services.common.application.exception;

public class BadGatewayException extends RuntimeException{

    private final ErrorCode errorCode;

    public BadGatewayException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
