package com.services.api.common.security.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.services.api.common.security.dto.SessionUser;
import com.services.core.common.persistence.entity.member.Member;
import com.services.core.common.persistence.entity.member.Role;
import com.services.core.common.persistence.repository.member.MemberRepository;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

  @Mock private MemberRepository memberRepository;

  @InjectMocks private CustomOAuth2UserService customOAuth2UserService;

  @Test
  @DisplayName("구글 OAuth2 로그인 시 신규 사용자면 저장하고 SessionUser를 반환한다")
  void processOAuth2User_newMember() {
    // Given
    String email = "test@example.com";
    String name = "Test User";
    String providerId = "google-12345";

    Map<String, Object> attributes =
        Map.of(
            "sub", providerId,
            "name", name,
            "email", email);

    OAuth2UserRequest userRequest = mockOAuth2UserRequest("sub");
    OAuth2User oAuth2User = new DefaultOAuth2User(Collections.emptyList(), attributes, "sub");

    Member member = Member.builder().email(email).providerId(providerId).role(Role.USER).build();

    when(memberRepository.findByEmail(email)).thenReturn(Optional.empty());
    when(memberRepository.save(any(Member.class))).thenReturn(member);

    // When
    OAuth2User result = customOAuth2UserService.processOAuth2User(userRequest, oAuth2User);

    // Then
    assertThat(result).isInstanceOf(SessionUser.class);
    SessionUser sessionUser = (SessionUser) result;
    assertThat(sessionUser.getEmail()).isEqualTo(email);
    assertThat(sessionUser.getAttributes()).containsAllEntriesOf(attributes);

    verify(memberRepository).save(any(Member.class));
  }

  @Test
  @DisplayName("구글 OAuth2 로그인 시 기존 사용자면 정보를 유지하고 SessionUser를 반환한다")
  void processOAuth2User_existingMember() {
    // Given
    String email = "existing@example.com";
    String providerId = "google-67890";

    Map<String, Object> attributes =
        Map.of(
            "sub", providerId,
            "email", email);

    OAuth2UserRequest userRequest = mockOAuth2UserRequest("sub");
    OAuth2User oAuth2User = new DefaultOAuth2User(Collections.emptyList(), attributes, "sub");

    Member existingMember =
        Member.builder().email(email).providerId(providerId).role(Role.USER).build();

    when(memberRepository.findByEmail(email)).thenReturn(Optional.of(existingMember));
    when(memberRepository.save(any(Member.class))).thenReturn(existingMember);

    // When
    OAuth2User result = customOAuth2UserService.processOAuth2User(userRequest, oAuth2User);

    // Then
    assertThat(result).isInstanceOf(SessionUser.class);
    verify(memberRepository).save(any(Member.class));
  }

  private OAuth2UserRequest mockOAuth2UserRequest(String userNameAttributeName) {
    OAuth2UserRequest userRequest = mock(OAuth2UserRequest.class);
    ClientRegistration clientRegistration =
        ClientRegistration.withRegistrationId("google")
            .clientId("client-id")
            .clientSecret("client-secret")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("http://localhost:8080/login/oauth2/code/google")
            .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
            .tokenUri("https://www.googleapis.com/oauth2/v4/token")
            .userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
            .userNameAttributeName(userNameAttributeName)
            .build();

    when(userRequest.getClientRegistration()).thenReturn(clientRegistration);
    return userRequest;
  }
}
