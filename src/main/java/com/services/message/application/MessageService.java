package com.services.message.application;

import com.services.common.application.exception.BadRequestException;
import com.services.common.application.exception.ErrorCode;
import com.services.message.presentation.dto.MessageRequest;
import com.services.message.util.WebUtils;
import com.services.message.validator.MessageValidator;
import jakarta.servlet.http.HttpServletRequest;
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

    public void saveMessage(MessageRequest body, HttpServletRequest request) {
        if (MessageValidator.isValid(body.getContent())) {
            createAndStoreMessage(body.getContent(), request);
        } else {
            throw new BadRequestException(ErrorCode.MESSAGE_INVALID_REQUEST);
        }
    }

    private void createAndStoreMessage(String content, HttpServletRequest request) {
        String clientIp = WebUtils.getClientIp(request);
        Message message = new Message(content, clientIp);
        messageStore.put(LAST_MESSAGE, message);
    }

    public String getMessage() {
        return Optional.ofNullable(messageStore.get(LAST_MESSAGE))
                .map(Message::getContent)
                .orElseGet(String::new);
    }
}