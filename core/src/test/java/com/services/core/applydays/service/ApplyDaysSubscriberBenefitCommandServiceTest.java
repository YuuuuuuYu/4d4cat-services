package com.services.core.applydays.service;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.services.core.applydays.entity.ApplyDaysMemberBenefitType;
import com.services.core.applydays.repository.ApplicationRepository;
import com.services.core.applydays.repository.ApplyDaysMemberBenefitRepository;
import com.services.core.common.persistence.entity.member.Member;
import com.services.core.common.persistence.entity.member.Role;
import com.services.core.fixture.ApplyDaysFixtures;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApplyDaysSubscriberBenefitCommandServiceTest {

  @Mock private ApplicationRepository applicationRepository;
  @Mock private ApplyDaysMemberBenefitRepository memberBenefitRepository;

  private ApplyDaysSubscriberBenefitCommandService subscriberBenefitCommandService;

  @BeforeEach
  void setUp() {
    subscriberBenefitCommandService =
        new ApplyDaysSubscriberBenefitCommandService(applicationRepository, memberBenefitRepository);
  }

  @Test
  @DisplayName("서류 합격 이력 10건을 공유한 REVIEWER에게 무료 기여자 구독 혜택을 등록한다")
  void grantIfEligible_reviewerWithTenApprovedDocumentPassedApplications() {
    // given
    Member reviewer = reviewer();
    given(applicationRepository.countApprovedDocumentPassedApplicationsByMemberId(reviewer.getId()))
        .willReturn(10L);

    // when
    subscriberBenefitCommandService.grantIfEligible(reviewer);

    // then
    verify(memberBenefitRepository)
        .grantIfAbsent(
            ArgumentMatchers.any(UUID.class),
            ArgumentMatchers.eq(reviewer.getId()),
            ArgumentMatchers.eq(ApplyDaysMemberBenefitType.SUBSCRIBER_ACCESS.name()));
  }

  @Test
  @DisplayName("서류 합격 이력 공유가 10건 미만인 REVIEWER에게는 혜택을 등록하지 않는다")
  void grantIfEligible_reviewerWithFewerThanTenApprovedDocumentPassedApplications() {
    // given
    Member reviewer = reviewer();
    given(applicationRepository.countApprovedDocumentPassedApplicationsByMemberId(reviewer.getId()))
        .willReturn(9L);

    // when
    subscriberBenefitCommandService.grantIfEligible(reviewer);

    // then
    verifyNoInteractions(memberBenefitRepository);
  }

  @Test
  @DisplayName("결제 구독자도 기여 조건을 달성하면 구독 만료 후를 위해 무료 혜택을 등록한다")
  void grantIfEligible_paidSubscriberWithEnoughContributions() {
    // given
    Member subscriber = ApplyDaysFixtures.createMember("subscriber@example.com", Role.SUBSCRIBER);
    ApplyDaysFixtures.setId(subscriber, UUID.randomUUID());
    given(applicationRepository.countApprovedDocumentPassedApplicationsByMemberId(subscriber.getId()))
        .willReturn(10L);

    // when
    subscriberBenefitCommandService.grantIfEligible(subscriber);

    // then
    verify(memberBenefitRepository)
        .grantIfAbsent(
            ArgumentMatchers.any(UUID.class),
            ArgumentMatchers.eq(subscriber.getId()),
            ArgumentMatchers.eq(ApplyDaysMemberBenefitType.SUBSCRIBER_ACCESS.name()));
  }

  private Member reviewer() {
    Member reviewer = ApplyDaysFixtures.createMember("reviewer@example.com", Role.REVIEWER);
    ApplyDaysFixtures.setId(reviewer, UUID.randomUUID());
    return reviewer;
  }
}
