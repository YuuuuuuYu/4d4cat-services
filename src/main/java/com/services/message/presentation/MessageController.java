package com.services.message.presentation;

import com.services.message.application.Message;
import com.services.message.application.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

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
    public ResponseEntity<Void> saveMessage(@RequestParam("content") String content, HttpServletRequest request) {
        messageService.saveMessage(content, request);
        return ResponseEntity.ok().build();
    }
}