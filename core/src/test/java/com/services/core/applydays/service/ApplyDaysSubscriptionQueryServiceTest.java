package com.services.core.applydays.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.services.core.applydays.dto.SubscriptionManageData;
import com.services.core.applydays.entity.subscription.ApplyDaysPayment;
import com.services.core.applydays.entity.subscription.ApplyDaysPaymentMethod;
import com.services.core.applydays.entity.subscription.ApplyDaysSubscription;
import com.services.core.applydays.entity.subscription.ApplyDaysSubscriptionPlan;
import com.services.core.applydays.repository.ApplyDaysPaymentMethodRepository;
import com.services.core.applydays.repository.ApplyDaysPaymentRepository;
import com.services.core.applydays.repository.ApplyDaysSubscriptionPlanRepository;
import com.services.core.applydays.repository.ApplyDaysSubscriptionRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApplyDaysSubscriptionQueryServiceTest {

  @Mock private ApplyDaysSubscriptionRepository subscriptionRepository;
  @Mock private ApplyDaysSubscriptionPlanRepository planRepository;
  @Mock private ApplyDaysPaymentRepository paymentRepository;
  @Mock private ApplyDaysPaymentMethodRepository paymentMethodRepository;

  private ApplyDaysSubscriptionQueryService queryService;

  @BeforeEach
  void setUp() {
    queryService =
        new ApplyDaysSubscriptionQueryService(
            subscriptionRepository, planRepository, paymentRepository, paymentMethodRepository);
  }

  @Test
  @DisplayName("활성화된 구독 플랜 목록을 조회한다")
  void getActivePlans_success() {
    // given
    ApplyDaysSubscriptionPlan plan =
        ApplyDaysSubscriptionPlan.builder().name("ApplyDays Premium").price(16500L).build();
    given(planRepository.findAllByDeletedFalse()).willReturn(List.of(plan));

    // when
    List<ApplyDaysSubscriptionPlan> plans = queryService.getActivePlans();

    // then
    assertThat(plans).hasSize(1);
    assertThat(plans.get(0).getName()).isEqualTo("ApplyDays Premium");
    verify(planRepository).findAllByDeletedFalse();
  }

  @Test
  @DisplayName("회원 ID로 구독 정보를 성공적으로 조회한다")
  void getSubscriptionByMemberId_success() {
    // given
    UUID memberId = UUID.randomUUID();
    ApplyDaysSubscription subscription = ApplyDaysSubscription.builder().memberId(memberId).build();
    given(subscriptionRepository.findByMemberId(memberId)).willReturn(Optional.of(subscription));

    // when
    Optional<ApplyDaysSubscription> result = queryService.getSubscriptionByMemberId(memberId);

    // then
    assertThat(result).isPresent();
    assertThat(result.get().getMemberId()).isEqualTo(memberId);
    verify(subscriptionRepository).findByMemberId(memberId);
  }

  @Test
  @DisplayName("회원 ID로 결제 이력을 최신순으로 조회한다")
  void getPaymentHistory_success() {
    // given
    UUID memberId = UUID.randomUUID();
    ApplyDaysPayment payment = ApplyDaysPayment.builder().memberId(memberId).amount(16500L).build();
    given(paymentRepository.findAllByMemberIdOrderByCreatedAtDesc(memberId))
        .willReturn(List.of(payment));

    // when
    List<ApplyDaysPayment> history = queryService.getPaymentHistory(memberId);

    // then
    assertThat(history).hasSize(1);
    assertThat(history.get(0).getAmount()).isEqualTo(16500L);
    verify(paymentRepository).findAllByMemberIdOrderByCreatedAtDesc(memberId);
  }

  @Test
  @DisplayName("회원 ID로 구독, 결제수단, 결제내역 통합 데이터를 성공적으로 조회한다")
  void getSubscriptionManageInfoData_success() {
    // given
    UUID memberId = UUID.randomUUID();
    ApplyDaysSubscription subscription = ApplyDaysSubscription.builder().memberId(memberId).build();
    ApplyDaysPaymentMethod paymentMethod =
        ApplyDaysPaymentMethod.builder().memberId(memberId).billingKey("bk_123").build();
    ApplyDaysPayment payment = ApplyDaysPayment.builder().memberId(memberId).amount(16500L).build();

    given(subscriptionRepository.findByMemberId(memberId)).willReturn(Optional.of(subscription));
    given(paymentMethodRepository.findByMemberIdAndDeletedFalse(memberId))
        .willReturn(Optional.of(paymentMethod));
    given(paymentRepository.findAllByMemberIdOrderByCreatedAtDesc(memberId))
        .willReturn(List.of(payment));

    // when
    SubscriptionManageData data = queryService.getSubscriptionManageInfoData(memberId);

    // then
    assertThat(data).isNotNull();
    assertThat(data.subscription()).contains(subscription);
    assertThat(data.paymentMethod()).contains(paymentMethod);
    assertThat(data.paymentHistories()).hasSize(1);
    assertThat(data.hasBillingKey()).isTrue();
  }
}
