package com.services.api.applydays.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.services.api.applydays.dto.PresignedUrlResponse;
import com.services.api.applydays.service.VerificationCommandService;
import com.services.api.applydays.service.VerificationQueryService;
import com.services.api.common.security.handler.OAuth2SuccessHandler;
import com.services.api.common.security.jwt.JwtProvider;
import com.services.api.common.security.service.CustomOAuth2UserService;
import com.services.core.common.infrastructure.RedisDataStorage;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(VerificationController.class)
class VerificationControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private VerificationCommandService verificationCommandService;
  @MockitoBean private VerificationQueryService verificationQueryService;

  @MockitoBean private CustomOAuth2UserService customOAuth2UserService;
  @MockitoBean private OAuth2SuccessHandler oAuth2SuccessHandler;
  @MockitoBean private JwtProvider jwtProvider;
  @MockitoBean private RedisDataStorage redisDataStorage;
  @MockitoBean private MeterRegistry meterRegistry;

  @Test
  @WithMockUser(username = "test@example.com", roles = "USER")
  @DisplayName("인증 이미지를 업로드한다")
  void upload_image_success() throws Exception {
    // given
    UUID applicationId = UUID.randomUUID();
    UUID imageId = UUID.randomUUID();
    MockMultipartFile file =
        new MockMultipartFile("file", "test.png", "image/png", "test content".getBytes());

    given(verificationCommandService.uploadVerificationImage(any(), any(), any()))
        .willReturn(imageId);

    // when & then
    mockMvc
        .perform(
            multipart("/applydays/verification/applications/{applicationId}/images", applicationId)
                .file(file)
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(201))
        .andExpect(jsonPath("$.data").value(imageId.toString()));
  }

  @Test
  @WithMockUser(username = "test@example.com", roles = "USER")
  @DisplayName("이미지 업로드를 위한 Presigned URL을 발급한다")
  void get_presigned_url_success() throws Exception {
    // given
    UUID applicationId = UUID.randomUUID();
    UUID imageId = UUID.randomUUID();
    PresignedUrlResponse response =
        PresignedUrlResponse.builder()
            .presignedUrl("http://presigned-url.com")
            .key("images/test.png")
            .imageId(imageId)
            .build();

    given(verificationCommandService.getPresignedUrl(any(), any(), any(), any()))
        .willReturn(response);

    // when & then
    mockMvc
        .perform(
            post(
                    "/applydays/verification/applications/{applicationId}/images/presigned-url",
                    applicationId)
                .param("fileName", "test.png")
                .param("contentType", "image/png")
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200))
        .andExpect(jsonPath("$.data.presignedUrl").value("http://presigned-url.com"))
        .andExpect(jsonPath("$.data.imageId").value(imageId.toString()));
  }
}
