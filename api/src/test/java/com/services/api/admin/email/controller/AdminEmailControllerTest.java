package com.services.api.admin.email.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.services.api.admin.email.dto.ManualEmailRequest;
import com.services.api.admin.email.service.AdminEmailService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminEmailControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private AdminEmailService adminEmailService;

  @Test
  @DisplayName("수동 메일 발송 요청 시 AdminEmailService를 호출한다")
  @WithMockUser(roles = "ADMIN")
  void sendManualEmail_shouldInvokeService() throws Exception {
    // given
    ManualEmailRequest request =
        new ManualEmailRequest(List.of("test@example.com"), "Subject", "Body");

    // when & then
    mockMvc
        .perform(
            post("/admin/emails/send-manual")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());

    verify(adminEmailService).sendManualEmail(any(ManualEmailRequest.class));
  }
}
