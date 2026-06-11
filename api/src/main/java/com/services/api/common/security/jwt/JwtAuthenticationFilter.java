package com.services.api.common.security.jwt;

import com.services.core.common.infrastructure.RedisDataStorage;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtProvider jwtProvider;
  private final RedisDataStorage redisDataStorage;
  private final RoleHierarchy roleHierarchy;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String token = resolveToken(request);

    if (StringUtils.hasText(token) && jwtProvider.validateToken(token)) {
      if (isBlacklisted(token)) {
        log.warn("Attempted access with blacklisted token");
      } else {
        Authentication auth = getAuthentication(token);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
      }
    }

    filterChain.doFilter(request, response);
  }

  private String resolveToken(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");
    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
      return bearerToken.substring(7);
    }
    return null;
  }

  private boolean isBlacklisted(String token) {
    if (redisDataStorage.getCache("JWT_BLACKLIST:" + token).isPresent()) {
      return true;
    }

    String email = jwtProvider.getEmail(token);
    return redisDataStorage
        .getCache("USER_REVOKED_AT:" + email)
        .map(
            obj -> {
              long revokedAt = Long.parseLong(obj.toString());
              long issuedAt = jwtProvider.getIssuedAt(token).getTime();
              return issuedAt < revokedAt;
            })
        .orElse(false);
  }

  private Authentication getAuthentication(String token) {
    String email = jwtProvider.getEmail(token);
    String role = jwtProvider.getRole(token);

    if (role != null && !role.startsWith("ROLE_")) {
      role = "ROLE_" + role;
    }

    log.debug("Authenticating user: {}, role: {}", email, role);

    Collection<? extends GrantedAuthority> authorities =
        roleHierarchy.getReachableGrantedAuthorities(
            Collections.singletonList(new SimpleGrantedAuthority(role)));

    return new UsernamePasswordAuthenticationToken(email, "", authorities);
  }
}
