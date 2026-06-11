package com.services.api.common.security;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.services.api.common.security.jwt.JwtProvider;
import com.services.core.common.infrastructure.RedisDataStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OAuth2IntegrationTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private RedisDataStorage redisDataStorage;

  @MockitoBean private JwtProvider jwtProvider;

  @Test
  @DisplayName("보호된 리소스에 접근 시 401 Unauthorized를 반환한다")
  void accessProtectedResource_shouldReturn401() throws Exception {
    mockMvc.perform(get("/admin/any")).andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("구글 로그인 시작 엔드포인트가 활성화되어 있다")
  void googleLoginInitiation_shouldRedirectToGoogle() throws Exception {
    mockMvc
        .perform(get("/oauth2/authorization/google"))
        .andExpect(status().isFound())
        .andExpect(header().string("Location", containsString("accounts.google.com")));
  }
}
