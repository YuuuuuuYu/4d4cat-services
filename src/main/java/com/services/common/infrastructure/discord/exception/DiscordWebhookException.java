package com.services.common.infrastructure.discord.exception;

import com.services.common.application.exception.BadGatewayException;
import com.services.common.application.exception.ErrorCode;

public class DiscordWebhookException extends BadGatewayException {
  public DiscordWebhookException(ErrorCode errorCode) {
    super(errorCode);
  }
}
