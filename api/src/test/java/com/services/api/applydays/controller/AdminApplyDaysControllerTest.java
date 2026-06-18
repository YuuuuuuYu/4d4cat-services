package com.services.api.applydays.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.services.api.applydays.service.AdminApplyDaysCommandService;
import com.services.api.applydays.service.AdminApplyDaysQueryService;
import com.services.api.common.infrastructure.external.redis.RedisMessagePublisher;
import com.services.api.common.security.handler.OAuth2SuccessHandler;
import com.services.api.common.security.jwt.JwtProvider;
import com.services.api.common.security.service.CustomOAuth2UserService;
import com.services.core.applydays.repository.VerificationImageRepository;
import com.services.core.common.infrastructure.RedisDataStorage;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminApplyDaysController.class)
class AdminApplyDaysControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ClientRegistrationRepository clientRegistrationRepository;
  @MockitoBean private AdminApplyDaysCommandService adminApplyDaysCommandService;
  @MockitoBean private AdminApplyDaysQueryService adminApplyDaysQueryService;
  @MockitoBean private VerificationImageRepository verificationImageRepository;
  @MockitoBean private RedisMessagePublisher redisMessagePublisher;

  @MockitoBean private CustomOAuth2UserService customOAuth2UserService;
  @MockitoBean private OAuth2SuccessHandler oAuth2SuccessHandler;
  @MockitoBean private JwtProvider jwtProvider;
  @MockitoBean private RedisDataStorage redisDataStorage;
  @MockitoBean private MeterRegistry meterRegistry;

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("대기 중인 인증 요청 목록을 조회한다")
  void getPendingRequests_success() throws Exception {
    given(adminApplyDaysQueryService.getPendingRequests(any(Pageable.class)))
        .willReturn(new SliceImpl<>(List.of()));

    mockMvc
        .perform(get("/admin/applydays/requests/pending"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("인증 요청을 승인한다")
  void approveRequest_success() throws Exception {
    UUID requestId = UUID.randomUUID();

    mockMvc
        .perform(
            post("/admin/applydays/requests/{id}/approve", requestId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(202));
  }
}
