package com.services.core.common.notification.email;

import com.services.core.common.dto.BaseResponse;
import com.services.core.common.notification.email.woorimail.WoorimailClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService {

  private final WoorimailClient woorimailClient;

  public BaseResponse<String> sendEmail(String to, String nickname, String subject, String body) {
    try {
      BaseResponse<String> response = woorimailClient.sendEmail(to, nickname, subject, body);
      log.info("Email send attempt finished for: {}. Status: {}", to, response.getStatus());
      return response;
    } catch (Exception e) {
      log.error("Unexpected error during email sending to: {}", to, e);
      throw new RuntimeException("Email sending failed", e);
    }
  }
}
