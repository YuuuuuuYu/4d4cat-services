package com.services.api.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.services.core.common.exception.UnauthorizedException;
import com.services.core.common.infrastructure.RedisDataStorage;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ApplyDaysSessionGrantTest {
  @Test
  void exchangesOnlyWithTheMatchingVerifierAndOnlyOnce() throws Exception {
    RedisDataStorage storage = mock(RedisDataStorage.class);
    String code = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    String key =
        "APPLYDAYS_AUTH_GRANT:"
            + Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                    MessageDigest.getInstance("SHA-256")
                        .digest(code.getBytes(StandardCharsets.US_ASCII)));
    String verifier = "a".repeat(43);
    String challenge =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(
                MessageDigest.getInstance("SHA-256")
                    .digest(verifier.getBytes(StandardCharsets.US_ASCII)));
    String value = challenge + ":test-token";
    when(storage.<String>getCache(key)).thenReturn(Optional.of(value));
    when(storage.takeString(key)).thenReturn(Optional.of(value), Optional.empty());

    assertThatThrownBy(() -> ApplyDaysSessionGrant.exchange(storage, code, "b".repeat(43)))
        .isInstanceOf(UnauthorizedException.class);
    assertThat(ApplyDaysSessionGrant.exchange(storage, code, verifier)).isEqualTo("test-token");
    assertThatThrownBy(() -> ApplyDaysSessionGrant.exchange(storage, code, verifier))
        .isInstanceOf(UnauthorizedException.class);
  }
}
