package com.services.api.admin.email.service;

import com.services.api.admin.email.dto.ManualEmailRequest;
import com.services.core.common.notification.email.EmailNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminEmailService {

  private final EmailNotificationService emailNotificationService;

  public void sendManualEmail(ManualEmailRequest request) {
    for (String to : request.toEmails()) {
      emailNotificationService.sendEmail(to, "User", request.subject(), request.body());
    }
  }
}
