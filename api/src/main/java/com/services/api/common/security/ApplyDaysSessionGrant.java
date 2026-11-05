package com.services.api.common.security;

import com.services.core.common.exception.ErrorCode;
import com.services.core.common.exception.UnauthorizedException;
import com.services.core.common.infrastructure.RedisDataStorage;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.web.util.UriComponentsBuilder;

public final class ApplyDaysSessionGrant {
  public static final String CHALLENGE = "applydays_challenge";
  private static final String CACHE_PREFIX = "APPLYDAYS_AUTH_GRANT:";

  private ApplyDaysSessionGrant() {}

  public static boolean isProof(String value) {
    return value != null && value.matches("[A-Za-z0-9_-]{43}");
  }

  public static String redirect(
      RedisDataStorage storage, String uri, String challenge, String token) {
    var target = UriComponentsBuilder.fromUriString(uri).replaceQuery(null);
    if (!isProof(challenge) || token == null) {
      return target.fragment("error=login_required").build().toUriString();
    }
    String code = UUID.randomUUID().toString();
    storage.setCache(CACHE_PREFIX + hash(code), challenge + ":" + token, 60, TimeUnit.SECONDS);
    return target.fragment("code=" + code).build().toUriString();
  }

  public static String exchange(RedisDataStorage storage, String code, String verifier) {
    if (code == null || !code.matches("[0-9a-f-]{36}") || !isProof(verifier)) {
      throw new UnauthorizedException(ErrorCode.UNAUTHORIZED);
    }
    String prefix = hash(verifier) + ":";
    String key = CACHE_PREFIX + hash(code);
    if (storage.<String>getCache(key).filter(value -> value.startsWith(prefix)).isEmpty()) {
      throw new UnauthorizedException(ErrorCode.UNAUTHORIZED);
    }
    return storage
        .takeString(key)
        .filter(value -> value.startsWith(prefix))
        .map(value -> value.substring(prefix.length()))
        .orElseThrow(() -> new UnauthorizedException(ErrorCode.UNAUTHORIZED));
  }

  private static String hash(String value) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.US_ASCII));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
