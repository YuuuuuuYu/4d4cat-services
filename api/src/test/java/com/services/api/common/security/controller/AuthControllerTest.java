package com.services.api.common.security.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.services.api.common.security.controller.AuthController.WithdrawRequest;
import com.services.api.common.security.jwt.JwtProvider;
import com.services.api.common.security.service.MemberService;
import com.services.core.common.infrastructure.RedisDataStorage;
import com.services.core.common.persistence.repository.member.MemberRepository;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private JwtProvider jwtProvider;

  @MockitoBean private RedisDataStorage redisDataStorage;

  @MockitoBean private MemberRepository memberRepository;

  @MockitoBean private MemberService memberService;

  @Test
  @DisplayName("전체 토큰 무효화 요청 시 Redis에 타임스탬프를 저장한다")
  void revokeAll_shouldStoreTimestampInRedis() throws Exception {
    // given
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken("test@example.com", "", null);

    // when & then
    mockMvc
        .perform(
            post("/auth/revoke-all")
                .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").value("All tokens have been revoked successfully"));

    verify(redisDataStorage)
        .setCache(eq("USER_REVOKED_AT:test@example.com"), anyString(), eq(2L), eq(TimeUnit.DAYS));
    verify(redisDataStorage).deleteCache("REFRESH_TOKEN:test@example.com");
  }

  @Test
  @DisplayName("회원 탈퇴 요청 시 MemberService.withdraw를 호출한다")
  void withdraw_shouldInvokeMemberService() throws Exception {
    // given
    String email = "test@example.com";
    WithdrawRequest request = new WithdrawRequest("OTHER", "Personal reasons");
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(email, "", null);

    // when & then
    mockMvc
        .perform(
            post("/auth/withdraw")
                .with(SecurityMockMvcRequestPostProcessors.authentication(auth))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").value("Withdrawn successfully"));

    verify(memberService).withdraw(email, "OTHER", "Personal reasons");
  }
}
