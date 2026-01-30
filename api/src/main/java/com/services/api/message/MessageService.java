package com.services.api.message;

import com.services.core.aop.NotifyDiscord;
import com.services.core.exception.BadRequestException;
import com.services.core.exception.ErrorCode;
import com.services.core.infrastructure.RedisMessageStorage;
import com.services.core.message.MessageRequest;
import com.services.core.message.MessageValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MessageService {

  private final RedisMessageStorage redisMessageStorage;

  @NotifyDiscord(
      taskName = "Message Stored",
      startLog = "Update Message",
      errorLog = "Failed to update message: %s")
  public void saveMessage(MessageRequest body) {
    if (MessageValidator.isValid(body.getContent())) {
      redisMessageStorage.saveMessage(body.getContent());
    } else {
      throw new BadRequestException(ErrorCode.MESSAGE_INVALID_REQUEST);
    }
  }

  public String getMessage() {
    return redisMessageStorage.getMessage().orElse("");
  }
}
