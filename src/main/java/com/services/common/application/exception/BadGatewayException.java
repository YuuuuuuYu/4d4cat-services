package com.services.common.application.exception;

public class BadGatewayException extends RuntimeException implements CustomException {

  private final ErrorCode errorCode;

  public BadGatewayException(ErrorCode errorCode) {
    super(errorCode.getMessageKey());
    this.errorCode = errorCode;
  }

  @Override
  public ErrorCode getErrorCode() {
    return errorCode;
  }
}
