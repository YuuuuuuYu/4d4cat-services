package com.services.message.presentation.dto;

import lombok.Getter;

@Getter
public class MessageRequest {

  private String content;

  public MessageRequest(String content) {
    this.content = content;
  }
}
