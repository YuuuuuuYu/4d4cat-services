package com.services.api.common.security.handler;

import com.services.api.common.security.dto.SessionUser;
import com.services.api.common.security.jwt.JwtProvider;
import com.services.core.common.infrastructure.RedisDataStorage;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

  private final JwtProvider jwtProvider;
  private final RedisDataStorage redisDataStorage;

  @Value("${app.oauth2.redirect-uri}")
  private String redirectUri;

  @Value("${jwt.refresh-expiration}")
  private long refreshTokenValidity;

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException {

    SessionUser user = (SessionUser) authentication.getPrincipal();
    String accessToken = jwtProvider.createAccessToken(user.getEmail(), user.getRole());
    String refreshToken = jwtProvider.createRefreshToken(user.getEmail());

    redisDataStorage.setCache(
        "REFRESH_TOKEN:" + user.getEmail(),
        refreshToken,
        refreshTokenValidity,
        TimeUnit.MILLISECONDS);

    String targetUrl =
        UriComponentsBuilder.fromUriString(redirectUri)
            .queryParam("accessToken", accessToken)
            .queryParam("refreshToken", refreshToken)
            .build()
            .toUriString();

    getRedirectStrategy().sendRedirect(request, response, targetUrl);
  }
}
