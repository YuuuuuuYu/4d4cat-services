package com.services.api.common.security.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.services.core.common.exception.NotFoundException;
import com.services.core.common.infrastructure.RedisDataStorage;
import com.services.core.common.persistence.entity.member.Member;
import com.services.core.common.persistence.entity.member.Role;
import com.services.core.common.persistence.entity.member.WithdrawLog;
import com.services.core.common.persistence.repository.member.MemberRepository;
import com.services.core.common.persistence.repository.member.WithdrawLogRepository;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

  @Mock private MemberRepository memberRepository;
  @Mock private WithdrawLogRepository withdrawLogRepository;
  @Mock private RedisDataStorage redisDataStorage;

  @InjectMocks private MemberService memberService;

  @Test
  @DisplayName("회원 탈퇴 성공 - 데이터 익명화 및 토큰 만료")
  void withdraw_success() {
    // given
    String email = "test@example.com";
    Member member = Member.builder().email(email).name("Test User").role(Role.USER).build();
    ReflectionTestUtils.setField(member, "id", UUID.randomUUID());

    when(memberRepository.findByEmail(email)).thenReturn(Optional.of(member));

    // when
    memberService.withdraw(email, "OTHER", "Reason detail");

    // then
    // 1. Log verification
    ArgumentCaptor<WithdrawLog> logCaptor = ArgumentCaptor.forClass(WithdrawLog.class);
    verify(withdrawLogRepository).save(logCaptor.capture());
    assertThat(logCaptor.getValue().getReasonCategory()).isEqualTo("OTHER");
    assertThat(logCaptor.getValue().getReasonDetail()).isEqualTo("Reason detail");

    // 2. Anonymization & Delete verification
    assertThat(member.getEmail()).isNull();
    assertThat(member.getName()).isNull();
    verify(memberRepository).delete(member);

    // 3. Token Cleanup verification
    verify(redisDataStorage).deleteCache("REFRESH_TOKEN:" + email);
    verify(redisDataStorage)
        .setCache(eq("USER_REVOKED_AT:" + email), anyString(), eq(2L), eq(TimeUnit.DAYS));
  }

  @Test
  @DisplayName("회원 탈퇴 실패 - 사용자를 찾을 수 없음")
  void withdraw_userNotFound() {
    // given
    String email = "nonexistent@example.com";
    when(memberRepository.findByEmail(email)).thenReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> memberService.withdraw(email, "OTHER", "Detail"))
        .isInstanceOf(NotFoundException.class);
  }
}
