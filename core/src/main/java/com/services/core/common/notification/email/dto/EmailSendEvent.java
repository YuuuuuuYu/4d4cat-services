package com.services.core.common.notification.email.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailSendEvent {
  private String toEmail;
  private String toNickname;
  private String subject;
  private String content;

  @Builder.Default private LocalDateTime scheduledAt = LocalDateTime.now();
}
