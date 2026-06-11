package com.services.api.common.security.dto;

import com.services.core.common.persistence.entity.member.Member;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

@Getter
public class SessionUser implements OAuth2User, Serializable {
  private final String name;
  private final String email;
  private final String role;
  private final Map<String, Object> attributes;

  public SessionUser(Member member, Map<String, Object> attributes) {
    this.name = member.getName();
    this.email = member.getEmail();
    this.role = member.getRole().getKey();
    this.attributes = attributes;
  }

  @Override
  public Map<String, Object> getAttributes() {
    return attributes;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return Collections.singleton(new SimpleGrantedAuthority(role));
  }

  @Override
  public String getName() {
    return name;
  }
}
