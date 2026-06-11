package com.services.core.applydays.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.services.core.applydays.entity.Application;
import com.services.core.applydays.entity.VerificationRequest;
import com.services.core.applydays.entity.VerificationStatus;
import com.services.core.applydays.repository.ApplicationRepository;
import com.services.core.applydays.repository.VerificationRequestRepository;
import com.services.core.common.persistence.entity.member.Member;
import com.services.core.common.persistence.entity.member.Role;
import com.services.core.common.persistence.repository.member.MemberRepository;
import com.services.core.fixture.ApplyDaysFixtures;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApplyDaysWorkerServiceTest {

  @Mock private ApplicationRepository applicationRepository;
  @Mock private VerificationRequestRepository verificationRequestRepository;
  @Mock private MemberRepository memberRepository;

  @InjectMocks private ApplyDaysWorkerService applyDaysWorkerService;

  @Test
  @DisplayName("지원서 승인 처리 시 상태가 변경되고 멤버 권한이 상승한다")
  void processApproval_success() {
    // given
    UUID applicationId = UUID.randomUUID();
    UUID memberId = UUID.randomUUID();

    VerificationRequest request =
        VerificationRequest.builder().applicationId(applicationId).memberId(memberId).build();

    Application application = ApplyDaysFixtures.createApplication("naver", 1L);
    ApplyDaysFixtures.setId(application, applicationId);

    Member member = ApplyDaysFixtures.createMember("test@test.com", Role.USER);
    ApplyDaysFixtures.setId(member, memberId);

    when(verificationRequestRepository.findByApplicationId(applicationId))
        .thenReturn(Optional.of(request));
    when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
    when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));

    // when
    applyDaysWorkerService.processApproval(applicationId);

    // then
    assertThat(request.getStatus()).isEqualTo(VerificationStatus.APPROVED);
    assertThat(application.getVerificationStatus()).isEqualTo(VerificationStatus.APPROVED);
    assertThat(application.getAccessPassword()).isNotNull();
    assertThat(member.getRole()).isEqualTo(Role.REVIEWER);
  }
}
