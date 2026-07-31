package com.services.core.applydays.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.services.core.applydays.entity.subscription.ApplyDaysSubscription;
import com.services.core.applydays.entity.subscription.ApplyDaysSubscriptionPlan;
import com.services.core.applydays.entity.subscription.SubscriptionStatus;
import com.services.core.applydays.event.SubscriptionCanceledEvent;
import com.services.core.applydays.event.SubscriptionExpiredEvent;
import com.services.core.applydays.repository.ApplyDaysPaymentRepository;
import com.services.core.applydays.repository.ApplyDaysSubscriptionPlanRepository;
import com.services.core.applydays.repository.ApplyDaysSubscriptionRepository;
import com.services.core.applydays.repository.VerificationRequestRepository;
import com.services.core.common.exception.BadRequestException;
import com.services.core.common.infrastructure.external.portone.PortOneClient;
import com.services.core.common.infrastructure.external.portone.PortOneClient.PortOnePaymentResponse;
import com.services.core.common.persistence.entity.member.Member;
import com.services.core.common.persistence.entity.member.Role;
import com.services.core.common.persistence.repository.member.MemberRepository;
import com.services.core.fixture.ApplyDaysFixtures;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class ApplyDaysSubscriptionCommandServiceTest {

  @Mock private ApplyDaysSubscriptionRepository subscriptionRepository;
  @Mock private ApplyDaysSubscriptionPlanRepository planRepository;
  @Mock private ApplyDaysPaymentRepository paymentRepository;
  @Mock private VerificationRequestRepository verificationRequestRepository;
  @Mock private MemberRepository memberRepository;
  @Mock private PortOneClient portOneClient;
  @Mock private ApplyDaysPaymentMethodQueryService paymentMethodQueryService;
  @Mock private ApplyDaysPaymentMethodCommandService paymentMethodCommandService;
  @Mock private ObjectProvider<ApplyDaysSubscriptionCommandService> selfProvider;
  @Mock private ApplicationEventPublisher eventPublisher;

  private MeterRegistry meterRegistry;
  private ApplyDaysSubscriptionCommandService subscriptionService;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    subscriptionService =
        new ApplyDaysSubscriptionCommandService(
            subscriptionRepository,
            planRepository,
            paymentRepository,
            verificationRequestRepository,
            memberRepository,
            portOneClient,
            paymentMethodQueryService,
            paymentMethodCommandService,
            selfProvider,
            meterRegistry,
            eventPublisher);
    lenient().when(selfProvider.getObject()).thenReturn(subscriptionService);
  }

  @Test
  @DisplayName("스케줄러 만료 처리 - CANCELED 상태이며 만료일이 지난 경우 EXPIRED 및 Role 원복")
  void expireSubscriptionIsolated_success() {
    // given
    UUID memberId = UUID.randomUUID();
    UUID planId = UUID.randomUUID();
    ApplyDaysSubscription subscription =
        ApplyDaysSubscription.builder()
            .memberId(memberId)
            .planId(planId)
            .status(SubscriptionStatus.CANCELED)
            .endDate(LocalDate.now().minusDays(1))
            .build();
    ApplyDaysSubscriptionPlan plan =
        ApplyDaysSubscriptionPlan.builder().name("Premium").price(16500L).build();
    Member member =
        Member.builder().email("user@example.com").name("Gildong").role(Role.SUBSCRIBER).build();

    given(subscriptionRepository.findWithLockByMemberId(memberId))
        .willReturn(Optional.of(subscription));
    given(planRepository.findById(planId)).willReturn(Optional.of(plan));
    given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

    // when
    subscriptionService.expireSubscriptionIsolated(memberId);

    // then
    assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
    assertThat(member.getRole()).isEqualTo(Role.USER);
    verify(eventPublisher).publishEvent(any(SubscriptionExpiredEvent.class));
  }

  @Test
  @DisplayName("스케줄러 만료 처리 - 만료일이 아직 도래하지 않은 경우 스킵")
  void expireSubscriptionIsolated_notYetExpired() {
    // given
    UUID memberId = UUID.randomUUID();
    ApplyDaysSubscription subscription =
        ApplyDaysSubscription.builder()
            .memberId(memberId)
            .status(SubscriptionStatus.CANCELED)
            .endDate(LocalDate.now().plusDays(10))
            .build();

    given(subscriptionRepository.findWithLockByMemberId(memberId))
        .willReturn(Optional.of(subscription));

    // when
    subscriptionService.expireSubscriptionIsolated(memberId);

    // then
    assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.CANCELED);
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  @DisplayName("카드 삭제 시 ACTIVE 구독인 경우 해지 예약(CANCELED) 처리")
  void scheduleCancellationOnCardDeleted_activeSubscription_schedulesCancel() {
    // given
    UUID memberId = UUID.randomUUID();
    UUID planId = UUID.randomUUID();
    ApplyDaysSubscription subscription =
        ApplyDaysSubscription.builder()
            .memberId(memberId)
            .planId(planId)
            .status(SubscriptionStatus.ACTIVE)
            .endDate(LocalDate.now().plusDays(10))
            .build();

    given(subscriptionRepository.findByMemberId(memberId)).willReturn(Optional.of(subscription));
    given(subscriptionRepository.save(any())).willAnswer(i -> i.getArgument(0));

    // when
    subscriptionService.scheduleCancellationOnCardDeleted(memberId);

    // then
    assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.CANCELED);
    assertThat(subscription.getNextBillingDate()).isNull();
  }

  @Test
  @DisplayName("구독 해지 성공 - 다음 결제예정일 제거, 만료일까지 유지, 이벤트 발행")
  void cancelSubscription_success() {
    // given
    UUID memberId = UUID.randomUUID();
    UUID planId = UUID.randomUUID();
    ApplyDaysSubscription subscription =
        ApplyDaysSubscription.builder()
            .memberId(memberId)
            .planId(planId)
            .status(SubscriptionStatus.ACTIVE)
            .startDate(LocalDate.now())
            .endDate(LocalDate.now().plusMonths(1))
            .nextBillingDate(LocalDate.now().plusMonths(1))
            .build();
    ApplyDaysSubscriptionPlan plan =
        ApplyDaysSubscriptionPlan.builder().name("Premium").price(16500L).build();
    Member member =
        Member.builder().email("user@example.com").name("Gildong").role(Role.SUBSCRIBER).build();

    given(subscriptionRepository.findWithLockByMemberId(memberId))
        .willReturn(Optional.of(subscription));
    given(subscriptionRepository.save(any())).willAnswer(i -> i.getArgument(0));
    given(planRepository.findById(any())).willReturn(Optional.of(plan));
    given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

    // when
    subscriptionService.cancelSubscription(memberId);

    // then
    assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.CANCELED);
    assertThat(subscription.getNextBillingDate()).isNull();
    verify(eventPublisher).publishEvent(any(SubscriptionCanceledEvent.class));
  }

  @Test
  @DisplayName("구독 해지 시 CANCELED 상태가 아닌 경우 예외 발생")
  void cancelSubscription_invalidStatus() {
    // given
    UUID memberId = UUID.randomUUID();
    ApplyDaysSubscription subscription =
        ApplyDaysSubscription.builder()
            .memberId(memberId)
            .status(SubscriptionStatus.PAYMENT_FAILED)
            .build();

    given(subscriptionRepository.findWithLockByMemberId(memberId))
        .willReturn(Optional.of(subscription));

    // when & then
    assertThatThrownBy(() -> subscriptionService.cancelSubscription(memberId))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  @DisplayName("구독 재개(Resume) 성공 - CANCELED 상태에서 ACTIVE로 복구, nextBillingDate 재설정")
  void resumeSubscription_success() {
    // given
    UUID memberId = UUID.randomUUID();
    UUID planId = UUID.randomUUID();
    ApplyDaysSubscription subscription =
        ApplyDaysSubscription.builder()
            .memberId(memberId)
            .planId(planId)
            .status(SubscriptionStatus.CANCELED)
            .startDate(LocalDate.now())
            .endDate(LocalDate.now().plusDays(10))
            .nextBillingDate(null)
            .build();

    Member member =
        Member.builder().email("user@example.com").name("Gildong").role(Role.USER).build();

    given(subscriptionRepository.findWithLockByMemberId(memberId))
        .willReturn(Optional.of(subscription));
    given(subscriptionRepository.save(any(ApplyDaysSubscription.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
    given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
    given(paymentMethodQueryService.hasPaymentMethod(memberId)).willReturn(true);

    // when
    ApplyDaysSubscription result = subscriptionService.resumeSubscription(memberId);

    // then
    assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    assertThat(result.getNextBillingDate()).isEqualTo(result.getEndDate().plusDays(1));
    assertThat(member.getRole()).isEqualTo(Role.SUBSCRIBER);
  }

  @Test
  @DisplayName("구독 재개 시 이미 만료기간이 경과한 경우 예외 발생")
  void resumeSubscription_expired() {
    // given
    UUID memberId = UUID.randomUUID();
    ApplyDaysSubscription subscription =
        ApplyDaysSubscription.builder()
            .memberId(memberId)
            .status(SubscriptionStatus.CANCELED)
            .endDate(LocalDate.now().minusDays(1))
            .build();

    given(subscriptionRepository.findWithLockByMemberId(memberId))
        .willReturn(Optional.of(subscription));

    // when & then
    assertThatThrownBy(() -> subscriptionService.resumeSubscription(memberId))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  @DisplayName("기존 미만료 구독이 있는 상태에서 새로운 결제 시 연장 처리")
  void processPayment_extendExistingSubscription() {
    // given
    UUID memberId = UUID.randomUUID();
    UUID planId = UUID.randomUUID();
    String billingKey = "billing_key_extend";
    String paymentId = "sub_" + memberId.toString() + "_extend_123";
    LocalDate existingEndDate = LocalDate.now().plusDays(15);

    ApplyDaysSubscription existingSubscription =
        ApplyDaysSubscription.builder()
            .memberId(memberId)
            .planId(planId)
            .status(SubscriptionStatus.ACTIVE)
            .startDate(LocalDate.now().minusDays(15))
            .endDate(existingEndDate)
            .nextBillingDate(existingEndDate)
            .build();

    ApplyDaysSubscriptionPlan plan =
        ApplyDaysSubscriptionPlan.builder()
            .name("Premium")
            .price(16500L)
            .billingCycleMonths(1)
            .build();

    Member member =
        Member.builder().email("user@example.com").name("Gildong").role(Role.SUBSCRIBER).build();

    given(paymentRepository.existsByPortonePaymentId(paymentId)).willReturn(false);
    given(subscriptionRepository.findByMemberId(memberId))
        .willReturn(Optional.of(existingSubscription));
    given(subscriptionRepository.findWithLockByMemberId(memberId))
        .willReturn(Optional.of(existingSubscription));
    given(planRepository.findById(any())).willReturn(Optional.of(plan));
    given(portOneClient.payWithBillingKey(anyString(), anyString(), anyLong(), anyString()))
        .willReturn(
            new PortOnePaymentResponse(
                paymentId,
                "PAID",
                new PortOneClient.Amount(16500L, "KRW"),
                billingKey,
                null,
                "url",
                "tx"));
    given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
    given(subscriptionRepository.save(any())).willAnswer(i -> i.getArgument(0));

    // when
    ApplyDaysSubscription result =
        subscriptionService.processPayment(memberId, billingKey, paymentId);

    // then
    assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    assertThat(result.getEndDate()).isEqualTo(existingEndDate.plusMonths(1));
    verify(paymentMethodCommandService).registerPaymentMethod(memberId, billingKey, null, null);
  }

  @Test
  @DisplayName("결제 실패 시 결제 수단이 등록되지 않는다")
  void processPayment_failed_doesNotRegisterPaymentMethod() {
    // given
    UUID memberId = UUID.randomUUID();
    UUID planId = UUID.randomUUID();
    String billingKey = "billing_key_fail";
    String paymentId = "sub_" + memberId.toString() + "_fail_123";

    ApplyDaysSubscriptionPlan plan =
        ApplyDaysSubscriptionPlan.builder()
            .name("Premium")
            .price(16500L)
            .billingCycleMonths(1)
            .build();
    ApplyDaysFixtures.setId(plan, planId);

    given(paymentRepository.existsByPortonePaymentId(paymentId)).willReturn(false);
    given(subscriptionRepository.findWithLockByMemberId(memberId)).willReturn(Optional.empty());
    given(planRepository.findById(any())).willReturn(Optional.of(plan));
    given(subscriptionRepository.save(any(ApplyDaysSubscription.class)))
        .willAnswer(
            i -> {
              ApplyDaysSubscription s = i.getArgument(0);
              ApplyDaysFixtures.setId(s, UUID.randomUUID());
              return s;
            });
    given(portOneClient.payWithBillingKey(anyString(), anyString(), anyLong(), anyString()))
        .willThrow(new RuntimeException("Payment API Error"));

    // when & then
    assertThatThrownBy(
            () -> subscriptionService.processPayment(memberId, billingKey, paymentId, planId))
        .isInstanceOf(BadRequestException.class);

    // verify payment method is NOT registered
    verify(paymentMethodCommandService, never())
        .registerPaymentMethod(any(UUID.class), anyString(), any(), any());
  }

  @Test
  @DisplayName("남은 이용기간이 있는 상태에서 재결제 시 기존 남은 기간에 1개월 추가 연장")
  void processPayment_accumulateRemainingPeriod() {
    // given
    UUID memberId = UUID.randomUUID();
    UUID planId = UUID.randomUUID();
    String billingKey = "billing_key_accumulate";
    String paymentId = "sub_" + memberId.toString() + "_accumulate_123";
    LocalDate initialStartDate = LocalDate.now().minusDays(15);
    LocalDate remainingEndDate = LocalDate.now().plusDays(15);

    ApplyDaysSubscription existingSubscription =
        ApplyDaysSubscription.builder()
            .memberId(memberId)
            .planId(planId)
            .status(SubscriptionStatus.CANCELED)
            .startDate(initialStartDate)
            .endDate(remainingEndDate)
            .nextBillingDate(null)
            .build();

    ApplyDaysSubscriptionPlan plan =
        ApplyDaysSubscriptionPlan.builder()
            .name("Premium")
            .price(16500L)
            .billingCycleMonths(1)
            .build();

    Member member =
        Member.builder().email("user@example.com").name("Gildong").role(Role.SUBSCRIBER).build();

    given(paymentRepository.existsByPortonePaymentId(paymentId)).willReturn(false);
    given(subscriptionRepository.findByMemberId(memberId))
        .willReturn(Optional.of(existingSubscription));
    given(subscriptionRepository.findWithLockByMemberId(memberId))
        .willReturn(Optional.of(existingSubscription));
    given(planRepository.findById(any())).willReturn(Optional.of(plan));
    given(portOneClient.payWithBillingKey(anyString(), anyString(), anyLong(), anyString()))
        .willReturn(
            new PortOnePaymentResponse(
                paymentId,
                "PAID",
                new PortOneClient.Amount(16500L, "KRW"),
                billingKey,
                null,
                "url",
                "tx"));
    given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
    given(subscriptionRepository.save(any())).willAnswer(i -> i.getArgument(0));

    // when
    ApplyDaysSubscription result =
        subscriptionService.processPayment(memberId, billingKey, paymentId);

    // then
    assertThat(result.getEndDate()).isEqualTo(remainingEndDate.plusMonths(1));
  }

  @Test
  @DisplayName("카드 삭제 이벤트 수신 시 ACTIVE 구독인 경우 CANCELED로 전환된다")
  void scheduleCancellationOnCardDeleted_activeSubscription() {
    // given
    UUID memberId = UUID.randomUUID();
    ApplyDaysSubscription activeSub =
        ApplyDaysSubscription.builder()
            .memberId(memberId)
            .status(SubscriptionStatus.ACTIVE)
            .startDate(LocalDate.now())
            .endDate(LocalDate.now().plusDays(30))
            .nextBillingDate(LocalDate.now().plusDays(30))
            .build();

    given(subscriptionRepository.findByMemberId(memberId)).willReturn(Optional.of(activeSub));

    // when
    subscriptionService.scheduleCancellationOnCardDeleted(memberId);

    // then
    assertThat(activeSub.getStatus()).isEqualTo(SubscriptionStatus.CANCELED);
    assertThat(activeSub.getNextBillingDate()).isNull();
    verify(subscriptionRepository).save(activeSub);
  }

  @Test
  @DisplayName("paymentId가 null 또는 빈 문자열일 경우 memberId 기반 paymentId가 자동 생성된다")
  void processPayment_autoGeneratesPaymentIdWhenNull() {
    // given
    UUID memberId = UUID.randomUUID();
    UUID planId = UUID.randomUUID();
    String billingKey = "billing_key_autogen";

    ApplyDaysSubscription existingSubscription =
        ApplyDaysSubscription.builder()
            .memberId(memberId)
            .planId(planId)
            .status(SubscriptionStatus.ACTIVE)
            .startDate(LocalDate.now())
            .endDate(LocalDate.now().plusDays(30))
            .nextBillingDate(LocalDate.now().plusDays(30))
            .build();

    ApplyDaysSubscriptionPlan plan =
        ApplyDaysSubscriptionPlan.builder()
            .name("Standard")
            .price(16500L)
            .billingCycleMonths(1)
            .build();
    ApplyDaysFixtures.setId(plan, planId);

    Member member =
        Member.builder().email("user@example.com").name("Gildong").role(Role.USER).build();

    given(paymentRepository.existsByPortonePaymentId(any())).willReturn(false);
    given(subscriptionRepository.findWithLockByMemberId(memberId))
        .willReturn(Optional.of(existingSubscription));
    given(planRepository.findById(any())).willReturn(Optional.of(plan));
    given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
    given(subscriptionRepository.save(any())).willAnswer(i -> i.getArgument(0));

    PortOnePaymentResponse mockResponse =
        new PortOnePaymentResponse(
            "sub_auto_123",
            "PAID",
            new PortOneClient.Amount(16500L, "KRW"),
            billingKey,
            null,
            "url",
            "tx_123");
    given(portOneClient.payWithBillingKey(anyString(), anyString(), anyLong(), anyString()))
        .willReturn(mockResponse);

    // when
    ApplyDaysSubscription result =
        subscriptionService.processPayment(memberId, billingKey, null, planId);

    // then
    assertThat(result).isNotNull();
    verify(portOneClient)
        .payWithBillingKey(
            eq(billingKey),
            argThat(id -> id != null && id.contains(memberId.toString())),
            eq(16500L),
            eq("Standard"));
  }
}
