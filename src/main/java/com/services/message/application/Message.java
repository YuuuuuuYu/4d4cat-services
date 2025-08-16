package com.services.message.application;

import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
public class Message {

    private final String content;
    private final String clientIp;
    private final LocalDateTime timestamp;

    public Message(String content, String clientIp) {
        this.content = content;
        this.clientIp = clientIp;
        this.timestamp = LocalDateTime.now();
    }
}