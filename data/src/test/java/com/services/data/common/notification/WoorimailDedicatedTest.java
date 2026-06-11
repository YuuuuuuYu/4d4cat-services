package com.services.data.common.notification;

import com.services.core.common.dto.BaseResponse;
import com.services.core.common.notification.email.woorimail.WoorimailClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class WoorimailDedicatedTest {

  @Autowired private WoorimailClient woorimailClient;

  @Test
  void sendTestEmail() {
    System.out.println(
        "Starting Woorimail Dedicated Test...");
    BaseResponse<String> response =
        woorimailClient.sendEmail(
            "dummy@dummy.com",
            "dummy",
            "Woorimail Dedicated Test",
            "<h1>Dedicated Test Success</h1><p>Testing</p>");
    System.out.println("Response Status: " + response.getStatus());
    System.out.println("Response Data: " + response.getData());
    if (response.getError() != null) {
      System.out.println("Error Code: " + response.getError().getCode());
      System.out.println("Error Message: " + response.getError().getMessage());
    }
  }
}
