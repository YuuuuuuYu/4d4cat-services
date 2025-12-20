package com.services.message.application;

import com.services.common.aop.NotifyDiscord;
import com.services.common.application.exception.BadRequestException;
import com.services.common.application.exception.ErrorCode;
import com.services.message.presentation.dto.MessageRequest;
import com.services.message.validator.MessageValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class MessageService {

  private static final String LAST_MESSAGE = "lastMessage";

  private final Map<String, Message> messageStore = new ConcurrentHashMap<>();

  @NotifyDiscord(
      taskName = "Message Stored",
      startLog = "Update Message",
      errorLog = "❌ Failed to update message: %s")
  public void saveMessage(MessageRequest body) {
    if (MessageValidator.isValid(body.getContent())) {
      createAndStoreMessage(body.getContent());
    } else {
      throw new BadRequestException(ErrorCode.MESSAGE_INVALID_REQUEST);
    }
  }

  private void createAndStoreMessage(String content) {
    Message message = new Message(content);
    messageStore.put(LAST_MESSAGE, message);
  }

  public String getMessage() {
    return Optional.ofNullable(messageStore.get(LAST_MESSAGE))
        .map(Message::getContent)
        .orElseGet(String::new);
  }
}
