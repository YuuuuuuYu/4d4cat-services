package com.services.api.common.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JwtProvider {

  private final SecretKey key;
  private final long accessTokenValidity;
  private final long refreshTokenValidity;

  public JwtProvider(
      @Value("${jwt.secret}") String secretKey,
      @Value("${jwt.expiration}") long accessTokenValidity,
      @Value("${jwt.refresh-expiration}") long refreshTokenValidity) {
    this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    this.accessTokenValidity = accessTokenValidity;
    this.refreshTokenValidity = refreshTokenValidity;
  }

  public String createAccessToken(String email, String role) {
    return createToken(email, role, accessTokenValidity);
  }

  public String createRefreshToken(String email) {
    return createToken(email, null, refreshTokenValidity);
  }

  private String createToken(String email, String role, long validity) {
    Date now = new Date();
    Date expiration = new Date(now.getTime() + validity);

    var builder = Jwts.builder().subject(email).issuedAt(now).expiration(expiration).signWith(key);

    if (role != null) {
      String finalRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;
      builder.claim("role", finalRole);
    }

    return builder.compact();
  }

  public String getEmail(String token) {
    return getClaims(token).getSubject();
  }

  public String getRole(String token) {
    return getClaims(token).get("role", String.class);
  }

  public Date getIssuedAt(String token) {
    return getClaims(token).getIssuedAt();
  }

  public long getRemainingExpirationTime(String token) {
    Date expiration = getClaims(token).getExpiration();
    return expiration.getTime() - System.currentTimeMillis();
  }

  public boolean validateToken(String token) {
    try {
      Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
      return true;
    } catch (SignatureException e) {
      log.error("Invalid JWT signature: {}", e.getMessage());
    } catch (MalformedJwtException e) {
      log.error("Invalid JWT token: {}", e.getMessage());
    } catch (ExpiredJwtException e) {
      log.error("JWT token is expired: {}", e.getMessage());
    } catch (UnsupportedJwtException e) {
      log.error("JWT token is unsupported: {}", e.getMessage());
    } catch (IllegalArgumentException e) {
      log.error("JWT claims string is empty: {}", e.getMessage());
    }
    return false;
  }

  private Claims getClaims(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }
}
