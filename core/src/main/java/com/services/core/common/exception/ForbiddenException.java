package com.services.core.common.exception;

public class ForbiddenException extends RuntimeException implements CustomException {

  private final ErrorCode errorCode;

  public ForbiddenException(ErrorCode errorCode) {
    super(errorCode.getMessageKey());
    this.errorCode = errorCode;
  }

  @Override
  public ErrorCode getErrorCode() {
    return errorCode;
  }
}
