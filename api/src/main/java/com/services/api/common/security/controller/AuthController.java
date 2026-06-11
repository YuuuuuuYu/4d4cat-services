package com.services.api.common.security.controller;

import com.services.api.common.security.jwt.JwtProvider;
import com.services.api.common.security.service.MemberService;
import com.services.core.common.dto.BaseResponse;
import com.services.core.common.exception.ErrorCode;
import com.services.core.common.exception.NotFoundException;
import com.services.core.common.exception.UnauthorizedException;
import com.services.core.common.infrastructure.RedisDataStorage;
import com.services.core.common.persistence.entity.member.Member;
import com.services.core.common.persistence.repository.member.MemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

  private final JwtProvider jwtProvider;
  private final RedisDataStorage redisDataStorage;
  private final MemberRepository memberRepository;
  private final MemberService memberService;

  public record MemberProfileResponse(String name, String email, String role) {}

  public record TokenRefreshRequest(String refreshToken) {}

  public record TokenResponse(String accessToken, String refreshToken) {}

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
        new MemberProfileResponse(member.getName(), member.getEmail(), member.getRole().getKey()));
  }

  @PostMapping("/refresh")
  public BaseResponse<TokenResponse> refresh(@RequestBody TokenRefreshRequest body) {
    String refreshToken = body.refreshToken();

    if (!jwtProvider.validateToken(refreshToken)) {
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

    return BaseResponse.of(HttpStatus.OK, new TokenResponse(newAccessToken, newRefreshToken));
  }

  @PostMapping("/logout")
  public BaseResponse<String> logout(
      HttpServletRequest request, @RequestBody(required = false) TokenRefreshRequest body) {
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

    if (body != null && StringUtils.hasText(body.refreshToken())) {
      if (jwtProvider.validateToken(body.refreshToken())) {
        String email = jwtProvider.getEmail(body.refreshToken());
        redisDataStorage.deleteCache("REFRESH_TOKEN:" + email);
      }
    }

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
