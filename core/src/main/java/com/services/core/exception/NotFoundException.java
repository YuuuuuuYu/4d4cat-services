package com.services.core.exception;

public class NotFoundException extends RuntimeException implements CustomException {

  private final ErrorCode errorCode;

  public NotFoundException(ErrorCode errorCode) {
    super(errorCode.getMessageKey());
    this.errorCode = errorCode;
  }

  @Override
  public ErrorCode getErrorCode() {
    return errorCode;
  }
}
