package com.services.api.applydays.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.services.api.applydays.dto.PaySubscriptionRequest;
import com.services.api.applydays.dto.PortOneWebhookRequest;
import com.services.api.applydays.dto.PreRegisterRequest;
import com.services.api.common.security.handler.OAuth2SuccessHandler;
import com.services.api.common.security.jwt.JwtProvider;
import com.services.api.common.security.service.CustomOAuth2UserService;
import com.services.core.applydays.dto.SubscriptionManageData;
import com.services.core.applydays.entity.subscription.ApplyDaysPayment;
import com.services.core.applydays.entity.subscription.ApplyDaysPaymentMethod;
import com.services.core.applydays.entity.subscription.ApplyDaysSubscription;
import com.services.core.applydays.entity.subscription.ApplyDaysSubscriptionPlan;
import com.services.core.applydays.entity.subscription.PaymentStatus;
import com.services.core.applydays.entity.subscription.SubscriptionStatus;
import com.services.core.applydays.service.ApplyDaysPaymentMethodQueryService;
import com.services.core.applydays.service.ApplyDaysSubscriptionCommandService;
import com.services.core.applydays.service.ApplyDaysSubscriptionQueryService;
import com.services.core.common.infrastructure.RedisDataStorage;
import com.services.core.common.persistence.entity.member.Member;
import com.services.core.common.persistence.entity.member.Role;
import com.services.core.common.persistence.repository.member.MemberRepository;
import com.services.core.fixture.ApplyDaysFixtures;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ApplyDaysSubscriptionController.class)
class ApplyDaysSubscriptionControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private ClientRegistrationRepository clientRegistrationRepository;
  @MockitoBean private ApplyDaysSubscriptionCommandService subscriptionService;
  @MockitoBean private ApplyDaysSubscriptionQueryService subscriptionQueryService;
  @MockitoBean private ApplyDaysPaymentMethodQueryService paymentMethodQueryService;
  @MockitoBean private MemberRepository memberRepository;

  @MockitoBean private CustomOAuth2UserService customOAuth2UserService;
  @MockitoBean private OAuth2SuccessHandler oAuth2SuccessHandler;
  @MockitoBean private JwtProvider jwtProvider;
  @MockitoBean private RedisDataStorage redisDataStorage;
  @MockitoBean private MeterRegistry meterRegistry;

  @Test
  @WithMockUser(username = "test@example.com", roles = "USER")
  @DisplayName("활성화된 구독 플랜 목록을 조회한다")
  void getActivePlans_success() throws Exception {
    // given
    ApplyDaysSubscriptionPlan plan =
        ApplyDaysSubscriptionPlan.builder()
            .name("ApplyDays Premium")
            .price(16500L)
            .billingCycleMonths(1)
            .build();
    UUID planId = UUID.randomUUID();
    ApplyDaysFixtures.setId(plan, planId);

    given(subscriptionQueryService.getActivePlans()).willReturn(List.of(plan));

    // when & then
    mockMvc
        .perform(get("/applydays/subscriptions/plans"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200))
        .andExpect(jsonPath("$.data[0].id").value(planId.toString()))
        .andExpect(jsonPath("$.data[0].name").value("ApplyDays Premium"))
        .andExpect(jsonPath("$.data[0].price").value(16500));
  }

  @Test
  @WithMockUser(username = "test@example.com", roles = "USER")
  @DisplayName("내 구독 정보를 조회한다")
  void getMySubscription_success() throws Exception {
    // given
    String email = "test@example.com";
    Member member = ApplyDaysFixtures.createMember(email, Role.SUBSCRIBER);
    UUID memberId = UUID.randomUUID();
    ApplyDaysFixtures.setId(member, memberId);

    UUID planId = UUID.randomUUID();
    ApplyDaysSubscription subscription =
        ApplyDaysSubscription.builder()
            .memberId(memberId)
            .planId(planId)
            .status(SubscriptionStatus.ACTIVE)
            .build();
    ApplyDaysFixtures.setId(subscription, UUID.randomUUID());

    given(memberRepository.findByEmail(email)).willReturn(Optional.of(member));
    given(subscriptionQueryService.getSubscriptionByMemberId(memberId))
        .willReturn(Optional.of(subscription));

    // when & then
    mockMvc
        .perform(get("/applydays/subscriptions/me"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200))
        .andExpect(jsonPath("$.data.memberId").value(memberId.toString()))
        .andExpect(jsonPath("$.data.planId").value(planId.toString()))
        .andExpect(jsonPath("$.data.status").value("ACTIVE"));
  }

  @Test
  @WithMockUser(username = "test@example.com", roles = "USER")
  @DisplayName("구독 관리 정보를 통합하여 일괄 조회한다")
  void getSubscriptionManageInfo_success() throws Exception {
    // given
    String email = "test@example.com";
    Member member = ApplyDaysFixtures.createMember(email, Role.SUBSCRIBER);
    UUID memberId = UUID.randomUUID();
    ApplyDaysFixtures.setId(member, memberId);

    UUID planId = UUID.randomUUID();
    ApplyDaysSubscription subscription =
        ApplyDaysSubscription.builder()
            .memberId(memberId)
            .planId(planId)
            .status(SubscriptionStatus.ACTIVE)
            .build();
    ApplyDaysFixtures.setId(subscription, UUID.randomUUID());

    ApplyDaysPaymentMethod paymentMethod =
        ApplyDaysPaymentMethod.builder()
            .memberId(memberId)
            .billingKey("bk_test")
            .cardCompany("Hyundai")
            .cardNumberMasked("1234-****-****-5678")
            .isDefault(true)
            .build();
    ApplyDaysFixtures.setId(paymentMethod, UUID.randomUUID());

    ApplyDaysPayment payment =
        ApplyDaysPayment.builder()
            .subscriptionId(subscription.getId())
            .memberId(memberId)
            .amount(16500L)
            .status(PaymentStatus.SUCCESS)
            .build();
    ApplyDaysFixtures.setId(payment, UUID.randomUUID());

    SubscriptionManageData manageData =
        new SubscriptionManageData(
            Optional.of(subscription), Optional.of(paymentMethod), List.of(payment), true);

    given(memberRepository.findByEmail(email)).willReturn(Optional.of(member));
    given(subscriptionQueryService.getSubscriptionManageInfoData(memberId)).willReturn(manageData);

    // when & then
    mockMvc
        .perform(get("/applydays/subscriptions/manage-info"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200))
        .andExpect(jsonPath("$.data.subscription.memberId").value(memberId.toString()))
        .andExpect(jsonPath("$.data.subscription.status").value("ACTIVE"))
        .andExpect(jsonPath("$.data.paymentMethod.cardCompany").value("Hyundai"))
        .andExpect(jsonPath("$.data.paymentHistories[0].amount").value(16500));
  }

  @Test
  @WithMockUser(username = "test@example.com", roles = "USER")
  @DisplayName("내 결제 이력을 조회한다")
  void getPaymentHistory_success() throws Exception {
    // given
    String email = "test@example.com";
    Member member = ApplyDaysFixtures.createMember(email, Role.SUBSCRIBER);
    UUID memberId = UUID.randomUUID();
    ApplyDaysFixtures.setId(member, memberId);

    UUID paymentId = UUID.randomUUID();
    String portonePaymentId = "sub_" + memberId + "_123456";
    String receiptUrl = "https://receipt.portone.io/v2/mock_receipt";

    ApplyDaysPayment payment =
        ApplyDaysPayment.builder()
            .subscriptionId(UUID.randomUUID())
            .memberId(memberId)
            .amount(16500L)
            .status(PaymentStatus.SUCCESS)
            .portonePaymentId(portonePaymentId)
            .portoneMerchantUid(portonePaymentId)
            .receiptUrl(receiptUrl)
            .build();
    ApplyDaysFixtures.setId(payment, paymentId);

    given(memberRepository.findByEmail(email)).willReturn(Optional.of(member));
    given(subscriptionQueryService.getPaymentHistory(memberId)).willReturn(List.of(payment));

    // when & then
    mockMvc
        .perform(get("/applydays/subscriptions/payments"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200))
        .andExpect(jsonPath("$.data[0].id").value(paymentId.toString()))
        .andExpect(jsonPath("$.data[0].amount").value(16500))
        .andExpect(jsonPath("$.data[0].status").value("SUCCESS"))
        .andExpect(jsonPath("$.data[0].portonePaymentId").value(portonePaymentId))
        .andExpect(jsonPath("$.data[0].receiptUrl").value(receiptUrl));
  }

  @Test
  @WithMockUser(username = "test@example.com", roles = "USER")
  @DisplayName("구독 사전 등록을 진행한다")
  void preRegister_success() throws Exception {
    // given
    String email = "test@example.com";
    UUID planId = UUID.randomUUID();
    PreRegisterRequest request = new PreRegisterRequest(planId);

    Member member = ApplyDaysFixtures.createMember(email, Role.USER);
    ApplyDaysFixtures.setId(member, UUID.randomUUID());

    ApplyDaysSubscription subscription =
        ApplyDaysSubscription.builder()
            .memberId(member.getId())
            .planId(planId)
            .status(SubscriptionStatus.PRE_REGISTERED)
            .build();
    ApplyDaysFixtures.setId(subscription, UUID.randomUUID());

    given(memberRepository.findByEmail(email)).willReturn(Optional.of(member));
    given(subscriptionService.preRegister(eq(member.getId()), eq(planId))).willReturn(subscription);

    // when & then
    mockMvc
        .perform(
            post("/applydays/subscriptions/pre-register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(201))
        .andExpect(jsonPath("$.data.status").value("PRE_REGISTERED"))
        .andExpect(jsonPath("$.data.planId").value(planId.toString()));
  }

  @Test
  @WithMockUser(username = "test@example.com", roles = "USER")
  @DisplayName("구독 결제를 완료 및 검증 처리한다")
  void paySubscription_success() throws Exception {
    // given
    String email = "test@example.com";
    String billingKey = "billing_12345";
    String paymentId = "sub_member_12345";
    PaySubscriptionRequest request = new PaySubscriptionRequest(billingKey, paymentId);

    Member member = ApplyDaysFixtures.createMember(email, Role.USER);
    UUID memberId = UUID.randomUUID();
    ApplyDaysFixtures.setId(member, memberId);

    ApplyDaysSubscription subscription =
        ApplyDaysSubscription.builder()
            .memberId(memberId)
            .planId(UUID.randomUUID())
            .status(SubscriptionStatus.ACTIVE)
            .build();
    ApplyDaysFixtures.setId(subscription, UUID.randomUUID());

    given(memberRepository.findByEmail(email)).willReturn(Optional.of(member));
    given(subscriptionService.processPayment(eq(memberId), eq(billingKey), eq(paymentId)))
        .willReturn(subscription);

    // when & then
    mockMvc
        .perform(
            post("/applydays/subscriptions/pay")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(201))
        .andExpect(jsonPath("$.data.status").value("ACTIVE"));
  }

  @Test
  @WithMockUser(username = "test@example.com", roles = "SUBSCRIBER")
  @DisplayName("구독 해지(취소)를 요청한다")
  void cancelSubscription_success() throws Exception {
    // given
    String email = "test@example.com";
    Member member = ApplyDaysFixtures.createMember(email, Role.SUBSCRIBER);
    UUID memberId = UUID.randomUUID();
    ApplyDaysFixtures.setId(member, memberId);

    given(memberRepository.findByEmail(email)).willReturn(Optional.of(member));

    // when & then
    mockMvc
        .perform(post("/applydays/subscriptions/cancel").with(csrf()))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200));

    verify(subscriptionService).cancelSubscription(eq(memberId));
  }

  @Test
  @WithMockUser(username = "webhook", roles = "USER")
  @DisplayName("포트원 결제완료 웹훅을 정상적으로 수신하고 처리한다")
  void handlePortOneWebhook_success() throws Exception {
    // given
    UUID memberId = UUID.randomUUID();
    String billingKey = "billing_webhook";
    String paymentId = "sub_" + memberId + "_123456";
    PortOneWebhookRequest request =
        new PortOneWebhookRequest(
            "Transaction.Paid",
            "2026-07-18T17:28:00.000Z",
            new PortOneWebhookRequest.WebhookData(paymentId, "store_123", "tx_123", billingKey));

    // when & then
    mockMvc
        .perform(
            post("/applydays/subscriptions/webhook")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200));

    verify(subscriptionService).processPayment(eq(memberId), isNull(), eq(paymentId));
  }

  @Test
  @WithMockUser(username = "test@example.com", roles = "SUBSCRIBER")
  @DisplayName("구독 재개(resume)를 요청한다")
  void resumeSubscription_success() throws Exception {
    // given
    String email = "test@example.com";
    Member member = ApplyDaysFixtures.createMember(email, Role.SUBSCRIBER);
    UUID memberId = UUID.randomUUID();
    ApplyDaysFixtures.setId(member, memberId);

    UUID planId = UUID.randomUUID();
    ApplyDaysSubscription subscription =
        ApplyDaysSubscription.builder()
            .memberId(memberId)
            .planId(planId)
            .status(SubscriptionStatus.ACTIVE)
            .build();
    ApplyDaysFixtures.setId(subscription, UUID.randomUUID());

    given(memberRepository.findByEmail(email)).willReturn(Optional.of(member));
    given(subscriptionService.resumeSubscription(eq(memberId))).willReturn(subscription);

    // when & then
    mockMvc
        .perform(post("/applydays/subscriptions/resume").with(csrf()))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200))
        .andExpect(jsonPath("$.data.status").value("ACTIVE"));

    verify(subscriptionService).resumeSubscription(eq(memberId));
  }

  @Test
  @WithMockUser(username = "test@example.com", roles = "SUBSCRIBER")
  @DisplayName("등록 카드 삭제(deleteCard)를 요청한다")
  void deleteCard_success() throws Exception {
    // given
    String email = "test@example.com";
    Member member = ApplyDaysFixtures.createMember(email, Role.SUBSCRIBER);
    UUID memberId = UUID.randomUUID();
    ApplyDaysFixtures.setId(member, memberId);

    given(memberRepository.findByEmail(email)).willReturn(Optional.of(member));

    // when & then
    mockMvc
        .perform(delete("/applydays/subscriptions/card").with(csrf()))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200));

    verify(subscriptionService).deleteCard(eq(memberId));
  }
}
