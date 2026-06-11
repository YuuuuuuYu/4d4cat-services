package com.services.api.common.security.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.services.api.common.security.dto.SessionUser;
import com.services.api.common.security.jwt.JwtProvider;
import com.services.core.common.infrastructure.RedisDataStorage;
import com.services.core.common.persistence.entity.member.Member;
import com.services.core.common.persistence.entity.member.Role;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OAuth2SuccessHandlerTest {

  @Mock private JwtProvider jwtProvider;
  @Mock private RedisDataStorage redisDataStorage;
  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;
  @Mock private Authentication authentication;
  @Mock private RedirectStrategy redirectStrategy;

  @InjectMocks private OAuth2SuccessHandler oAuth2SuccessHandler;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(
        oAuth2SuccessHandler, "redirectUri", "http://localhost:3000/login-success");
    ReflectionTestUtils.setField(oAuth2SuccessHandler, "refreshTokenValidity", 604800000L);
    oAuth2SuccessHandler.setRedirectStrategy(redirectStrategy);
  }

  @Test
  @DisplayName("로그인 성공 시 JWT를 생성하고 지정된 URL로 리다이렉트한다")
  void onAuthenticationSuccess_shouldRedirectWithToken() throws IOException {
    // Given
    String email = "test@example.com";
    String accessToken = "test-access-token";
    String refreshToken = "test-refresh-token";

    Member member = Member.builder().email(email).role(Role.USER).build();
    SessionUser sessionUser = new SessionUser(member, Map.of());

    when(authentication.getPrincipal()).thenReturn(sessionUser);
    when(jwtProvider.createAccessToken(eq(email), anyString())).thenReturn(accessToken);
    when(jwtProvider.createRefreshToken(eq(email))).thenReturn(refreshToken);

    // When
    oAuth2SuccessHandler.onAuthenticationSuccess(request, response, authentication);

    // Then
    ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
    verify(redirectStrategy).sendRedirect(eq(request), eq(response), urlCaptor.capture());

    String redirectedUrl = urlCaptor.getValue();
    assertThat(redirectedUrl).contains("/login-success");
    assertThat(redirectedUrl).contains("accessToken=" + accessToken);
    assertThat(redirectedUrl).contains("refreshToken=" + refreshToken);
  }
}
