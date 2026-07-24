package com.services.api.common.security.handler;

import com.services.api.common.annotation.AuditAction;
import com.services.api.common.security.dto.SessionUser;
import com.services.api.common.security.jwt.JwtProvider;
import com.services.api.common.security.service.MemberService;
import com.services.core.common.infrastructure.RedisDataStorage;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

  private static final Logger USER_AUDIT_LOGGER = LoggerFactory.getLogger("USER_AUDIT_LOGGER");

  private final JwtProvider jwtProvider;
  private final RedisDataStorage redisDataStorage;
  private final MemberService memberService;

  @Value("${app.oauth2.redirect-uri}")
  private String redirectUri;

  @Value("${jwt.refresh-expiration}")
  private long refreshTokenValidity;

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException {

    SessionUser user = (SessionUser) authentication.getPrincipal();
    try {
      String memberId = memberService.getMemberIdByEmail(user.getEmail());
      USER_AUDIT_LOGGER.info(
          "[USER_AUDIT] memberId={} action={} target={}", memberId, AuditAction.LOGIN_SUCCESS, "");
    } catch (Exception e) {
      USER_AUDIT_LOGGER.info(
          "[USER_AUDIT] memberId={} action={} target={}",
          "ANONYMOUS",
          AuditAction.LOGIN_SUCCESS,
          "");
    }

    String accessToken = jwtProvider.createAccessToken(user.getEmail(), user.getRole());
    String refreshToken = jwtProvider.createRefreshToken(user.getEmail());

    redisDataStorage.setCache(
        "REFRESH_TOKEN:" + user.getEmail(),
        refreshToken,
        refreshTokenValidity,
        TimeUnit.MILLISECONDS);

    ResponseCookie cookie =
        ResponseCookie.from("refreshToken", refreshToken)
            .httpOnly(true)
            .secure(request.isSecure())
            .path("/")
            .maxAge(refreshTokenValidity / 1000)
            .sameSite("Lax")
            .build();
    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

    String targetUrl =
        UriComponentsBuilder.fromUriString(redirectUri)
            .queryParam("accessToken", accessToken)
            .build()
            .toUriString();

    getRedirectStrategy().sendRedirect(request, response, targetUrl);
  }
}
