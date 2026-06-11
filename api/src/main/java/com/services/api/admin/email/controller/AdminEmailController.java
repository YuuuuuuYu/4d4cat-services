package com.services.api.admin.email.controller;

import com.services.api.admin.email.dto.ManualEmailRequest;
import com.services.api.admin.email.service.AdminEmailService;
import com.services.core.common.dto.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/emails")
@RequiredArgsConstructor
public class AdminEmailController {

  private final AdminEmailService adminEmailService;

  @PostMapping("/send-manual")
  public BaseResponse<Void> sendManualEmail(@RequestBody ManualEmailRequest request) {
    adminEmailService.sendManualEmail(request);
    return BaseResponse.of(HttpStatus.OK, null);
  }
}
