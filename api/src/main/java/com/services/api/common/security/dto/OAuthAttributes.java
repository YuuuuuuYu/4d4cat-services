package com.services.api.common.security.dto;

import com.services.core.common.persistence.entity.member.Member;
import com.services.core.common.persistence.entity.member.Role;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

@Getter
public class OAuthAttributes {
  private final Map<String, Object> attributes;
  private final String nameAttributeKey;
  private final String name;
  private final String email;
  private final String providerId;

  @Builder
  public OAuthAttributes(
      Map<String, Object> attributes,
      String nameAttributeKey,
      String name,
      String email,
      String providerId) {
    this.attributes = attributes;
    this.nameAttributeKey = nameAttributeKey;
    this.name = name;
    this.email = email;
    this.providerId = providerId;
  }

  public static OAuthAttributes of(
      String registrationId, String userNameAttributeName, Map<String, Object> attributes) {
    return ofGoogle(userNameAttributeName, attributes);
  }

  private static OAuthAttributes ofGoogle(
      String userNameAttributeName, Map<String, Object> attributes) {
    String name = (String) attributes.get("name");
    if (name == null || name.isEmpty()) {
      name = (String) attributes.get("given_name");
    }
    if (name == null || name.isEmpty()) {
      String email = (String) attributes.get("email");
      if (email != null && email.contains("@")) {
        name = email.split("@")[0];
      } else {
        name = "Google User";
      }
    }

    return OAuthAttributes.builder()
        .name(name)
        .email((String) attributes.get("email"))
        .providerId((String) attributes.get(userNameAttributeName))
        .attributes(attributes)
        .nameAttributeKey(userNameAttributeName)
        .build();
  }

  public Member toEntity(Role role) {
    return Member.builder().email(email).name(name).providerId(providerId).role(role).build();
  }
}
