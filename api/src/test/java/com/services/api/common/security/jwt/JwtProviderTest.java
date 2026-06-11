package com.services.api.common.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtProviderTest {

  private final JwtProvider jwtProvider =
      new JwtProvider("test-secret-key-12345678901234567890123456789012", 600000, 172800000);

  @Test
  @DisplayName("토큰에서 issuedAt 클레임을 추출할 수 있다")
  void getIssuedAt_shouldReturnDate() {
    // given
    String email = "test@example.com";
    String role = "USER";
    String token = jwtProvider.createAccessToken(email, role);

    // when
    Date issuedAt = jwtProvider.getIssuedAt(token);

    // then
    assertThat(issuedAt).isNotNull();
    assertThat(issuedAt.getTime()).isLessThanOrEqualTo(System.currentTimeMillis());
  }

  @Test
  @DisplayName("새로 생성된 토큰의 issuedAt은 현재 시간 부근이어야 한다")
  void createToken_shouldHaveCorrectIssuedAt() {
    // given
    long start = System.currentTimeMillis();
    String token = jwtProvider.createAccessToken("user@example.com", "USER");
    long end = System.currentTimeMillis();

    // when
    Date issuedAt = jwtProvider.getIssuedAt(token);

    // then
    assertThat(issuedAt.getTime()).isBetween(start - 1000, end + 1000);
  }
}
