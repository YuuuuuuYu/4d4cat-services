package com.services.api.applydays.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
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
import com.services.api.common.config.SecurityConfiguration;
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
import com.services.core.common.infrastructure.external.portone.PortOneClient;
import com.services.core.common.infrastructure.external.portone.PortOneClient.Amount;
import com.services.core.common.infrastructure.external.portone.PortOneClient.PortOnePaymentResponse;
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
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ApplyDaysSubscriptionController.class)
@Import(SecurityConfiguration.class)
@TestPropertySource(properties = {"app.portone.webhook-secret=test-secret"})
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

  @MockitoBean private PortOneClient portOneClient;

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
    given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
    given(jwtProvider.createAccessToken(eq(email), eq("ROLE_USER")))
        .willReturn("mock_access_token_pay");
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
        .andExpect(jsonPath("$.data.status").value("ACTIVE"))
        .andExpect(jsonPath("$.data.accessToken").value("mock_access_token_pay"));
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
  @DisplayName("인증 헤더가 없는 비인증 상태의 포트원 결제완료 웹훅을 정상적으로 수신하고 처리한다")
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

    PortOnePaymentResponse mockResponse =
        new PortOnePaymentResponse(
            paymentId, "PAID", new Amount(16500L, "KRW"), null, null, null, null);
    given(portOneClient.verifyPayment(eq(paymentId))).willReturn(mockResponse);

    ApplyDaysSubscription subscription =
        ApplyDaysSubscription.builder().memberId(memberId).planId(UUID.randomUUID()).build();
    given(subscriptionQueryService.getSubscriptionByMemberId(eq(memberId)))
        .willReturn(Optional.of(subscription));

    ApplyDaysSubscriptionPlan plan = ApplyDaysSubscriptionPlan.builder().price(16500L).build();
    given(subscriptionQueryService.getPlan(eq(subscription.getPlanId())))
        .willReturn(Optional.of(plan));

    // when & then
    mockMvc
        .perform(
            post("/applydays/subscriptions/webhook")
                .header("webhook-signature", "test-secret")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200));

    verify(portOneClient).verifyPayment(eq(paymentId));
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
    given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
    given(jwtProvider.createAccessToken(eq(email), eq("ROLE_SUBSCRIBER")))
        .willReturn("mock_access_token_resume");
    given(subscriptionService.resumeSubscription(eq(memberId))).willReturn(subscription);

    // when & then
    mockMvc
        .perform(post("/applydays/subscriptions/resume").with(csrf()))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200))
        .andExpect(jsonPath("$.data.status").value("ACTIVE"))
        .andExpect(jsonPath("$.data.accessToken").value("mock_access_token_resume"));

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

  @Test
  @DisplayName("포트원 웹훅 서명 검증이 실패하면 401 반환한다")
  void handlePortOneWebhook_invalidSignature() throws Exception {
    // given
    PortOneWebhookRequest request = new PortOneWebhookRequest("Transaction.Paid", "time", null);

    // when & then
    mockMvc
        .perform(
            post("/applydays/subscriptions/webhook")
                .header("webhook-signature", "invalid-signature")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(401));
  }

  @Test
  @DisplayName("이미 성공 처리된 결제는 멱등성 검증(패스) 처리한다")
  void handlePortOneWebhook_idempotencyPass() throws Exception {
    // given
    UUID memberId = UUID.randomUUID();
    String paymentId = "sub_" + memberId + "_123";
    PortOneWebhookRequest request =
        new PortOneWebhookRequest(
            "Transaction.Paid",
            "time",
            new PortOneWebhookRequest.WebhookData(paymentId, "store", "tx", "bk"));

    ApplyDaysPayment existingPayment =
        ApplyDaysPayment.builder().memberId(memberId).status(PaymentStatus.SUCCESS).build();
    ApplyDaysFixtures.setId(existingPayment, UUID.randomUUID());

    given(subscriptionQueryService.getPaymentByPortonePaymentId(eq(paymentId)))
        .willReturn(Optional.of(existingPayment));

    // when & then
    mockMvc
        .perform(
            post("/applydays/subscriptions/webhook")
                .header("webhook-signature", "test-secret")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isOk());

    verify(subscriptionService, never()).processPayment(any(), any(), any());
  }

  @Test
  @DisplayName("웹훅 2차 검증 시 결제 금액이 불일치하면 예외(400)를 반환한다")
  void handlePortOneWebhook_amountMismatch() throws Exception {
    // given
    UUID memberId = UUID.randomUUID();
    String paymentId = "sub_" + memberId + "_123";
    PortOneWebhookRequest request =
        new PortOneWebhookRequest(
            "Transaction.Paid",
            "time",
            new PortOneWebhookRequest.WebhookData(paymentId, "store", "tx", "bk"));

    PortOnePaymentResponse mockResponse =
        new PortOnePaymentResponse(
            paymentId,
            "PAID",
            new Amount(1000L, "KRW"),
            null,
            null,
            null,
            null); // Actual amount 1000
    given(portOneClient.verifyPayment(eq(paymentId))).willReturn(mockResponse);

    ApplyDaysSubscription subscription =
        ApplyDaysSubscription.builder().memberId(memberId).planId(UUID.randomUUID()).build();
    given(subscriptionQueryService.getSubscriptionByMemberId(eq(memberId)))
        .willReturn(Optional.of(subscription));

    ApplyDaysSubscriptionPlan plan =
        ApplyDaysSubscriptionPlan.builder().price(16500L).build(); // Expected amount 16500
    given(subscriptionQueryService.getPlan(eq(subscription.getPlanId())))
        .willReturn(Optional.of(plan));

    // when & then
    mockMvc
        .perform(
            post("/applydays/subscriptions/webhook")
                .header("webhook-signature", "test-secret")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(400));
  }

  @Test
  @DisplayName("지원하지 않는 포트원 웹훅 이벤트 타입 수신 시 결제 처리 없이 200 OK 반환한다")
  void handlePortOneWebhook_unsupportedEventType() throws Exception {
    // given
    PortOneWebhookRequest request =
        new PortOneWebhookRequest(
            "Transaction.Cancelled",
            "2026-07-18T17:28:00.000Z",
            new PortOneWebhookRequest.WebhookData(
                "sub_" + UUID.randomUUID() + "_123", "store", "tx", "bk"));

    // when & then
    mockMvc
        .perform(
            post("/applydays/subscriptions/webhook")
                .header("webhook-signature", "test-secret")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200));

    verify(subscriptionService, never()).processPayment(any(), any(), any());
  }

  @Test
  @DisplayName("paymentId에서 memberId 추출 실패 시 400 반환한다")
  void handlePortOneWebhook_invalidPaymentIdFormat() throws Exception {
    // given
    PortOneWebhookRequest request =
        new PortOneWebhookRequest(
            "Transaction.Paid",
            "2026-07-18T17:28:00.000Z",
            new PortOneWebhookRequest.WebhookData(
                "invalid_payment_id_format", "store", "tx", "bk"));

    // when & then
    mockMvc
        .perform(
            post("/applydays/subscriptions/webhook")
                .header("webhook-signature", "test-secret")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(400));

    verify(subscriptionService, never()).processPayment(any(), any(), any());
  }

  @Test
  @DisplayName("웹훅 검증 시 결제 상태가 PAID가 아닌 경우 400 반환한다")
  void handlePortOneWebhook_notPaidStatus() throws Exception {
    // given
    UUID memberId = UUID.randomUUID();
    String paymentId = "sub_" + memberId + "_123";
    PortOneWebhookRequest request =
        new PortOneWebhookRequest(
            "Transaction.Paid",
            "time",
            new PortOneWebhookRequest.WebhookData(paymentId, "store", "tx", "bk"));

    PortOnePaymentResponse mockResponse =
        new PortOnePaymentResponse(
            paymentId, "FAILED", new Amount(16500L, "KRW"), null, null, null, null);
    given(portOneClient.verifyPayment(eq(paymentId))).willReturn(mockResponse);

    // when & then
    mockMvc
        .perform(
            post("/applydays/subscriptions/webhook")
                .header("webhook-signature", "test-secret")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(400));

    verify(subscriptionService, never()).processPayment(any(), any(), any());
  }

  @Test
  @DisplayName("포트원 외부 API 호출 중 예외 발생 시 400 반환하며 안전하게 처리한다")
  void handlePortOneWebhook_portOneClientException() throws Exception {
    // given
    UUID memberId = UUID.randomUUID();
    String paymentId = "sub_" + memberId + "_123";
    PortOneWebhookRequest request =
        new PortOneWebhookRequest(
            "Transaction.Paid",
            "time",
            new PortOneWebhookRequest.WebhookData(paymentId, "store", "tx", "bk"));

    given(portOneClient.verifyPayment(eq(paymentId)))
        .willThrow(new RuntimeException("PortOne API Timeout"));

    // when & then
    mockMvc
        .perform(
            post("/applydays/subscriptions/webhook")
                .header("webhook-signature", "test-secret")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(400));

    verify(subscriptionService, never()).processPayment(any(), any(), any());
  }

  @Test
  @DisplayName("웹훅 검증 시 해당 회원의 구독 정보가 존재하지 않으면 400 반환한다")
  void handlePortOneWebhook_subscriptionNotFound() throws Exception {
    // given
    UUID memberId = UUID.randomUUID();
    String paymentId = "sub_" + memberId + "_123";
    PortOneWebhookRequest request =
        new PortOneWebhookRequest(
            "Transaction.Paid",
            "time",
            new PortOneWebhookRequest.WebhookData(paymentId, "store", "tx", "bk"));

    PortOnePaymentResponse mockResponse =
        new PortOnePaymentResponse(
            paymentId, "PAID", new Amount(16500L, "KRW"), null, null, null, null);
    given(portOneClient.verifyPayment(eq(paymentId))).willReturn(mockResponse);
    given(subscriptionQueryService.getSubscriptionByMemberId(eq(memberId)))
        .willReturn(Optional.empty());

    // when & then
    mockMvc
        .perform(
            post("/applydays/subscriptions/webhook")
                .header("webhook-signature", "test-secret")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(400));

    verify(subscriptionService, never()).processPayment(any(), any(), any());
  }

  @Test
  @DisplayName("비인증 사용자가 내 구독 정보를 조회 시 401 Unauthorized를 반환한다")
  void getMySubscription_unauthorized() throws Exception {
    // when & then
    mockMvc
        .perform(get("/applydays/subscriptions/me"))
        .andDo(print())
        .andExpect(status().isUnauthorized());
  }
}
