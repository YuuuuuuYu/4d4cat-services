package com.services.message.presentation;

import com.services.message.application.MessageService;
import com.services.message.presentation.dto.MessageRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MessageController {

  private final MessageService messageService;

  @GetMapping("/message")
  public ResponseEntity<String> home() {
    String message = messageService.getMessage();
    return ResponseEntity.ok(message);
  }

  @PostMapping("/message")
  public ResponseEntity<Void> saveMessage(@RequestBody MessageRequest body) {
    messageService.saveMessage(body);
    return ResponseEntity.ok().build();
  }
}
