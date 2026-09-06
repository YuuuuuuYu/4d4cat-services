package com.services.core.applydays.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import com.services.core.applydays.entity.ApplyDaysMemberBenefitType;
import com.services.core.applydays.repository.ApplyDaysMemberBenefitRepository;
import com.services.core.common.persistence.entity.member.Member;
import com.services.core.common.persistence.entity.member.Role;
import com.services.core.fixture.ApplyDaysFixtures;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApplyDaysSubscriberBenefitQueryServiceTest {

  @Mock private ApplyDaysMemberBenefitRepository memberBenefitRepository;

  private ApplyDaysSubscriberBenefitQueryService subscriberBenefitQueryService;

  @BeforeEach
  void setUp() {
    subscriberBenefitQueryService =
        new ApplyDaysSubscriberBenefitQueryService(memberBenefitRepository);
  }

  @Test
  @DisplayName("무료 기여자 구독 혜택이 등록된 REVIEWER에게 구독자 혜택을 부여한다")
  void hasSubscriberBenefit_reviewerWithEarnedSubscriberBenefit() {
    // given
    Member reviewer = reviewer();
    given(
            memberBenefitRepository.existsByMemberIdAndBenefitType(
                reviewer.getId(), ApplyDaysMemberBenefitType.SUBSCRIBER_ACCESS))
        .willReturn(true);

    // when
    boolean result = subscriberBenefitQueryService.hasSubscriberBenefit(reviewer);

    // then
    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("무료 기여자 구독 혜택이 없는 REVIEWER에게는 구독자 혜택을 부여하지 않는다")
  void hasSubscriberBenefit_reviewerWithoutEarnedSubscriberBenefit() {
    // given
    Member reviewer = reviewer();

    // when
    boolean result = subscriberBenefitQueryService.hasSubscriberBenefit(reviewer);

    // then
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("결제 구독자에게는 공유 이력 수와 관계없이 구독자 혜택을 유지한다")
  void hasSubscriberBenefit_paidSubscriber() {
    // given
    Member subscriber = ApplyDaysFixtures.createMember("subscriber@example.com", Role.SUBSCRIBER);

    // when
    boolean result = subscriberBenefitQueryService.hasSubscriberBenefit(subscriber);

    // then
    assertThat(result).isTrue();
    verifyNoInteractions(memberBenefitRepository);
  }

  private Member reviewer() {
    Member reviewer = ApplyDaysFixtures.createMember("reviewer@example.com", Role.REVIEWER);
    ApplyDaysFixtures.setId(reviewer, UUID.randomUUID());
    return reviewer;
  }
}
