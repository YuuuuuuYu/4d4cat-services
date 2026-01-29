package com.services.core.exception;

public class InternalServerException extends RuntimeException implements CustomException {

  private final ErrorCode errorCode;

  public InternalServerException(ErrorCode errorCode) {
    super(errorCode.getMessageKey());
    this.errorCode = errorCode;
  }

  @Override
  public ErrorCode getErrorCode() {
    return errorCode;
  }
}
