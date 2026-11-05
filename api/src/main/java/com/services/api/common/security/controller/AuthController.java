package com.services.api.common.security.controller;

import com.services.api.common.security.ApplyDaysSessionGrant;
import com.services.api.common.security.jwt.JwtProvider;
import com.services.api.common.security.service.MemberService;
import com.services.core.applydays.service.ApplyDaysSubscriberBenefitQueryService;
import com.services.core.common.dto.BaseResponse;
import com.services.core.common.exception.ErrorCode;
import com.services.core.common.exception.NotFoundException;
import com.services.core.common.exception.UnauthorizedException;
import com.services.core.common.infrastructure.RedisDataStorage;
import com.services.core.common.persistence.entity.member.Member;
import com.services.core.common.persistence.repository.member.MemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

  private final JwtProvider jwtProvider;
  private final RedisDataStorage redisDataStorage;
  private final MemberRepository memberRepository;
  private final MemberService memberService;
  private final ApplyDaysSubscriberBenefitQueryService subscriberBenefitQueryService;

  @Value("${app.oauth2.redirect-uri}")
  private String redirectUri;

  public record MemberProfileResponse(
      String name, String email, String role, boolean hasSubscriberBenefit) {}

  public record TokenRefreshRequest(String refreshToken) {}

  public record TokenResponse(String accessToken, String refreshToken) {}

  public record SessionExchangeRequest(String code, String verifier) {}

  public record AccessTokenResponse(String accessToken) {}

  public record WithdrawRequest(String reasonCategory, String reasonDetail) {}

  @GetMapping("/me")
  public BaseResponse<MemberProfileResponse> getMyProfile(@AuthenticationPrincipal String email) {
    if (email == null) {
      throw new UnauthorizedException(ErrorCode.UNAUTHORIZED);
    }

    Member member =
        memberRepository
            .findByEmail(email)
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

    return BaseResponse.of(
        HttpStatus.OK,
        new MemberProfileResponse(
            member.getName(),
            member.getEmail(),
            member.getRole().getKey(),
            subscriberBenefitQueryService.hasSubscriberBenefit(member)));
  }

  @PostMapping("/refresh")
  public BaseResponse<TokenResponse> refresh(
      HttpServletRequest request,
      HttpServletResponse response,
      @CookieValue(name = "refreshToken", required = false) String refreshToken) {

    if (!StringUtils.hasText(refreshToken) || !jwtProvider.validateToken(refreshToken)) {
      throw new UnauthorizedException(ErrorCode.UNAUTHORIZED);
    }

    String email = jwtProvider.getEmail(refreshToken);
    String savedToken =
        redisDataStorage.getCache("REFRESH_TOKEN:" + email).map(Object::toString).orElse(null);

    if (savedToken == null || !savedToken.equals(refreshToken)) {
      throw new UnauthorizedException(ErrorCode.UNAUTHORIZED);
    }

    Member member =
        memberRepository
            .findByEmail(email)
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

    String newAccessToken =
        jwtProvider.createAccessToken(member.getEmail(), member.getRole().getKey());
    String newRefreshToken = jwtProvider.createRefreshToken(member.getEmail());

    long expiration = jwtProvider.getRemainingExpirationTime(newRefreshToken);
    redisDataStorage.setCache(
        "REFRESH_TOKEN:" + email, newRefreshToken, expiration, TimeUnit.MILLISECONDS);

    ResponseCookie cookie =
        ResponseCookie.from("refreshToken", newRefreshToken)
            .httpOnly(true)
            .secure(request.isSecure())
            .path("/")
            .maxAge(expiration / 1000)
            .sameSite("Lax")
            .build();
    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

    return BaseResponse.of(HttpStatus.OK, new TokenResponse(newAccessToken, null));
  }

  @GetMapping("/session/continue")
  public void continueSession(
      HttpServletResponse response,
      @RequestParam(ApplyDaysSessionGrant.CHALLENGE) String challenge,
      @CookieValue(name = "refreshToken", required = false) String refreshToken)
      throws IOException {
    String token = null;
    if (ApplyDaysSessionGrant.isProof(challenge)
        && StringUtils.hasText(refreshToken)
        && jwtProvider.validateToken(refreshToken)) {
      String email = jwtProvider.getEmail(refreshToken);
      if (redisDataStorage
          .<String>getCache("REFRESH_TOKEN:" + email)
          .filter(refreshToken::equals)
          .isPresent()) {
        token =
            memberRepository
                .findByEmail(email)
                .map(
                    member ->
                        jwtProvider.createAccessToken(member.getEmail(), member.getRole().getKey()))
                .orElse(null);
      }
    }
    response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
    response.setHeader("Referrer-Policy", "no-referrer");
    response.sendRedirect(
        ApplyDaysSessionGrant.redirect(redisDataStorage, redirectUri, challenge, token));
  }

  @PostMapping("/session/exchange")
  public BaseResponse<AccessTokenResponse> exchangeSession(
      @RequestBody SessionExchangeRequest request, HttpServletResponse response) {
    response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
    String token =
        ApplyDaysSessionGrant.exchange(redisDataStorage, request.code(), request.verifier());
    return BaseResponse.of(HttpStatus.OK, new AccessTokenResponse(token));
  }

  @PostMapping("/logout")
  public BaseResponse<String> logout(
      HttpServletRequest request,
      HttpServletResponse response,
      @CookieValue(name = "refreshToken", required = false) String refreshToken) {
    String token = resolveToken(request);

    if (StringUtils.hasText(token) && jwtProvider.validateToken(token)) {
      long expiration = jwtProvider.getRemainingExpirationTime(token);
      if (expiration > 0) {
        redisDataStorage.setCache(
            "JWT_BLACKLIST:" + token, "logout", expiration, TimeUnit.MILLISECONDS);
      }

      String email = jwtProvider.getEmail(token);
      redisDataStorage.deleteCache("REFRESH_TOKEN:" + email);
    }

    if (StringUtils.hasText(refreshToken) && jwtProvider.validateToken(refreshToken)) {
      String email = jwtProvider.getEmail(refreshToken);
      redisDataStorage.deleteCache("REFRESH_TOKEN:" + email);
    }

    ResponseCookie cookie =
        ResponseCookie.from("refreshToken", "")
            .httpOnly(true)
            .secure(request.isSecure())
            .path("/")
            .maxAge(0)
            .sameSite("Lax")
            .build();
    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

    return BaseResponse.of(HttpStatus.OK, "Logged out successfully");
  }

  @PostMapping("/revoke-all")
  public BaseResponse<String> revokeAll(@AuthenticationPrincipal String email) {
    if (email == null) {
      throw new UnauthorizedException(ErrorCode.UNAUTHORIZED);
    }

    long now = System.currentTimeMillis();
    redisDataStorage.setCache("USER_REVOKED_AT:" + email, String.valueOf(now), 2, TimeUnit.DAYS);
    redisDataStorage.deleteCache("REFRESH_TOKEN:" + email);

    return BaseResponse.of(HttpStatus.OK, "All tokens have been revoked successfully");
  }

  @PostMapping("/withdraw")
  public BaseResponse<String> withdraw(
      @AuthenticationPrincipal String email, @RequestBody WithdrawRequest body) {
    if (email == null) {
      throw new UnauthorizedException(ErrorCode.UNAUTHORIZED);
    }

    memberService.withdraw(email, body.reasonCategory(), body.reasonDetail());
    return BaseResponse.of(HttpStatus.OK, "Withdrawn successfully");
  }

  private String resolveToken(HttpServletRequest request) {

    String bearerToken = request.getHeader("Authorization");
    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
      return bearerToken.substring(7);
    }
    return null;
  }
}
