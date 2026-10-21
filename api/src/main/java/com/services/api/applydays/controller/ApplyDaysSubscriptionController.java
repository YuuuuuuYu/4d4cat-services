package com.services.api.applydays.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.services.api.applydays.dto.PaySubscriptionRequest;
import com.services.api.applydays.dto.PaymentHistoryResponse;
import com.services.api.applydays.dto.PaymentMethodResponse;
import com.services.api.applydays.dto.PortOneWebhookRequest;
import com.services.api.applydays.dto.SubscriptionManageInfoResponse;
import com.services.api.applydays.dto.SubscriptionPlanResponse;
import com.services.api.applydays.dto.SubscriptionResponse;
import com.services.api.common.security.jwt.JwtProvider;
import com.services.core.applydays.dto.SubscriptionManageData;
import com.services.core.applydays.dto.SubscriptionPaymentId;
import com.services.core.applydays.entity.subscription.ApplyDaysPayment;
import com.services.core.applydays.entity.subscription.ApplyDaysSubscription;
import com.services.core.applydays.entity.subscription.ApplyDaysSubscriptionPlan;
import com.services.core.applydays.entity.subscription.PaymentStatus;
import com.services.core.applydays.service.ApplyDaysPaymentMethodQueryService;
import com.services.core.applydays.service.ApplyDaysSubscriptionCommandService;
import com.services.core.applydays.service.ApplyDaysSubscriptionQueryService;
import com.services.core.common.dto.BaseResponse;
import com.services.core.common.exception.BadRequestException;
import com.services.core.common.exception.ErrorCode;
import com.services.core.common.exception.NotFoundException;
import com.services.core.common.exception.UnauthorizedException;
import com.services.core.common.infrastructure.external.portone.PortOneClient;
import com.services.core.common.infrastructure.external.portone.PortOneClient.PortOnePaymentResponse;
import com.services.core.common.infrastructure.external.portone.PortOneWebhookVerifier;
import com.services.core.common.persistence.entity.member.Member;
import com.services.core.common.persistence.repository.member.MemberRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/applydays/subscriptions")
@RequiredArgsConstructor
public class ApplyDaysSubscriptionController {

  private final ApplyDaysSubscriptionCommandService subscriptionService;
  private final ApplyDaysSubscriptionQueryService subscriptionQueryService;
  private final ApplyDaysPaymentMethodQueryService paymentMethodQueryService;
  private final MemberRepository memberRepository;
  private final JwtProvider jwtProvider;
  private final PortOneClient portOneClient;
  private final ObjectMapper objectMapper;

  @Value("${app.portone.webhook-secret}")
  private String webhookSecret;

  @GetMapping("/plans")
  public BaseResponse<List<SubscriptionPlanResponse>> getActivePlans() {
    log.info("Request received to fetch active subscription plans");
    List<SubscriptionPlanResponse> plans =
        subscriptionQueryService.getActivePlans().stream()
            .map(SubscriptionPlanResponse::from)
            .toList();
    return BaseResponse.of(HttpStatus.OK, plans);
  }

  @GetMapping("/me")
  public BaseResponse<SubscriptionResponse> getMySubscription(Authentication authentication) {
    if (authentication == null) {
      log.warn("Unauthorized request to /me endpoint");
      throw new UnauthorizedException(ErrorCode.UNAUTHORIZED);
    }
    String email = authentication.getName();
    log.info("Request received to fetch subscription details for user: {}", email);

    Member member =
        memberRepository
            .findByEmail(email)
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

    boolean hasBillingKey = paymentMethodQueryService.hasPaymentMethod(member.getId());

    return subscriptionQueryService
        .getSubscriptionByMemberId(member.getId())
        .map(sub -> BaseResponse.of(HttpStatus.OK, SubscriptionResponse.from(sub, hasBillingKey)))
        .orElseGet(() -> BaseResponse.of(HttpStatus.OK, null));
  }

  @GetMapping("/manage-info")
  public BaseResponse<SubscriptionManageInfoResponse> getSubscriptionManageInfo(
      Authentication authentication) {
    if (authentication == null) {
      log.warn("Unauthorized request to /manage-info endpoint");
      throw new UnauthorizedException(ErrorCode.UNAUTHORIZED);
    }
    String email = authentication.getName();
    log.info("Request received to fetch subscription manage info for user: {}", email);

    Member member =
        memberRepository
            .findByEmail(email)
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

    SubscriptionManageData data =
        subscriptionQueryService.getSubscriptionManageInfoData(member.getId());

    SubscriptionResponse subscriptionResponse =
        data.subscription()
            .map(sub -> SubscriptionResponse.from(sub, data.hasBillingKey()))
            .orElse(null);

    PaymentMethodResponse paymentMethodResponse =
        data.paymentMethod().map(PaymentMethodResponse::from).orElse(null);

    List<PaymentHistoryResponse> paymentHistories =
        data.paymentHistories().stream().map(PaymentHistoryResponse::from).toList();

    return BaseResponse.of(
        HttpStatus.OK,
        new SubscriptionManageInfoResponse(
            subscriptionResponse, paymentMethodResponse, paymentHistories));
  }

  @GetMapping("/payments")
  public BaseResponse<List<PaymentHistoryResponse>> getPaymentHistory(
      Authentication authentication) {
    if (authentication == null) {
      throw new UnauthorizedException(ErrorCode.UNAUTHORIZED);
    }
    String email = authentication.getName();
    Member member =
        memberRepository
            .findByEmail(email)
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
    List<PaymentHistoryResponse> payments =
        subscriptionQueryService.getPaymentHistory(member.getId()).stream()
            .map(PaymentHistoryResponse::from)
            .toList();
    return BaseResponse.of(HttpStatus.OK, payments);
  }

  @PostMapping("/pay")
  public BaseResponse<SubscriptionResponse> paySubscription(
      Authentication authentication, @RequestBody PaySubscriptionRequest request) {

    if (authentication == null) {
      throw new UnauthorizedException(ErrorCode.UNAUTHORIZED);
    }
    String email = authentication.getName();
    log.info(
        "Payment subscription request received: email={}, paymentId={}, planId={}",
        email,
        request.paymentId(),
        request.planId());

    Member member =
        memberRepository
            .findByEmail(email)
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

    ApplyDaysSubscription subscription =
        subscriptionService.processPayment(
            member.getId(), request.billingKey(), request.paymentId(), request.planId());

    Member updatedMember = memberRepository.findById(member.getId()).orElse(member);
    boolean hasBillingKey = paymentMethodQueryService.hasPaymentMethod(member.getId());
    String accessToken =
        jwtProvider.createAccessToken(updatedMember.getEmail(), updatedMember.getRole().getKey());

    return BaseResponse.of(
        HttpStatus.CREATED, SubscriptionResponse.from(subscription, hasBillingKey, accessToken));
  }

  @PostMapping("/cancel")
  public BaseResponse<Void> cancelSubscription(Authentication authentication) {
    if (authentication == null) {
      throw new UnauthorizedException(ErrorCode.UNAUTHORIZED);
    }
    String email = authentication.getName();
    log.info("Subscription cancellation request received: email={}", email);

    Member member =
        memberRepository
            .findByEmail(email)
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

    subscriptionService.cancelSubscription(member.getId());

    return BaseResponse.of(HttpStatus.OK, null);
  }

  @PostMapping("/resume")
  public BaseResponse<SubscriptionResponse> resumeSubscription(Authentication authentication) {
    if (authentication == null) {
      throw new UnauthorizedException(ErrorCode.UNAUTHORIZED);
    }
    String email = authentication.getName();
    log.info("Subscription resume request received for user: {}", email);

    Member member =
        memberRepository
            .findByEmail(email)
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

    ApplyDaysSubscription subscription = subscriptionService.resumeSubscription(member.getId());

    Member updatedMember = memberRepository.findById(member.getId()).orElse(member);
    boolean hasBillingKey = paymentMethodQueryService.hasPaymentMethod(member.getId());
    String accessToken =
        jwtProvider.createAccessToken(updatedMember.getEmail(), updatedMember.getRole().getKey());

    return BaseResponse.of(
        HttpStatus.OK, SubscriptionResponse.from(subscription, hasBillingKey, accessToken));
  }

  @PostMapping("/webhook")
  public ResponseEntity<BaseResponse<Void>> handlePortOneWebhook(
      @RequestHeader(value = "webhook-signature", required = false) String signature,
      @RequestHeader(value = "webhook-id", required = false) String webhookId,
      @RequestHeader(value = "webhook-timestamp", required = false) String webhookTimestamp,
      @RequestBody String body) {
    if (!PortOneWebhookVerifier.verify(
        webhookSecret, webhookId, webhookTimestamp, signature, body)) {
      log.warn(
          "Invalid PortOne webhook signature: idPresent={}, timestampPresent={}, signaturePresent={}",
          webhookId != null,
          webhookTimestamp != null,
          signature != null);
      return webhookResponse(HttpStatus.UNAUTHORIZED);
    }
    PortOneWebhookRequest request;
    try {
      request = objectMapper.readValue(body, PortOneWebhookRequest.class);
    } catch (JsonProcessingException e) {
      throw new BadRequestException(ErrorCode.INVALID_REQUEST);
    }
    if (request == null) {
      throw new BadRequestException(ErrorCode.INVALID_REQUEST);
    }
    log.info("PortOne V2 webhook received: type={}", request.type());

    if ("Transaction.Paid".equalsIgnoreCase(request.type())) {
      if (request.data() == null) {
        throw new BadRequestException(ErrorCode.INVALID_REQUEST);
      }
      String paymentId = request.data().paymentId();
      UUID memberId = SubscriptionPaymentId.memberId(paymentId);
      if (memberId == null) {
        log.warn("Could not extract memberId from paymentId: {}", paymentId);
        return webhookResponse(HttpStatus.BAD_REQUEST);
      }

      ApplyDaysPayment existingPayment =
          subscriptionQueryService.getPaymentByPortonePaymentId(paymentId).orElse(null);
      if (existingPayment != null && existingPayment.getStatus() == PaymentStatus.SUCCESS) {
        log.info(
            "Webhook: Payment {} already processed successfully, idempotency check passed",
            paymentId);
        return webhookResponse(HttpStatus.OK);
      }

      try {
        PortOnePaymentResponse verifiedPayment = portOneClient.verifyPayment(paymentId);
        if (!"PAID".equalsIgnoreCase(verifiedPayment.status())) {
          log.warn("PortOne payment 2nd verification failed: status={}", verifiedPayment.status());
          return webhookResponse(HttpStatus.BAD_REQUEST);
        }

        ApplyDaysSubscription subscription =
            subscriptionQueryService.getSubscriptionByMemberId(memberId).orElse(null);
        if (subscription == null) {
          log.warn("Webhook: Subscription not found for memberId={}", memberId);
          // The first payment webhook may arrive before /pay commits the subscription.
          return webhookResponse(HttpStatus.SERVICE_UNAVAILABLE);
        }

        ApplyDaysSubscriptionPlan plan =
            subscriptionQueryService.getPlan(subscription.getPlanId()).orElse(null);
        if (plan == null) {
          log.warn("Webhook: Subscription plan not found for planId={}", subscription.getPlanId());
          return webhookResponse(HttpStatus.SERVICE_UNAVAILABLE);
        }

        if (!paymentId.equals(verifiedPayment.id())
            || verifiedPayment.amount() == null
            || verifiedPayment.amount().total() != plan.getPrice()
            || !"KRW".equals(verifiedPayment.currency())) {
          log.warn(
              "Webhook: Payment amount verification failed. expected={}, actual={}",
              plan.getPrice(),
              verifiedPayment.amount() != null ? verifiedPayment.amount().total() : null);
          return webhookResponse(HttpStatus.BAD_REQUEST);
        }

        subscriptionService.savePaymentAndActivate(
            memberId, null, paymentId, verifiedPayment, plan.getId());
      } catch (Exception e) {
        log.error("PortOne payment 2nd verification exception for paymentId={}", paymentId, e);
        return webhookResponse(HttpStatus.SERVICE_UNAVAILABLE);
      }

      log.info("PortOne V2 Webhook processed successfully for memberId={}", memberId);
    } else {
      log.info("Ignoring webhook type: {}", request.type());
    }

    return webhookResponse(HttpStatus.OK);
  }

  private ResponseEntity<BaseResponse<Void>> webhookResponse(HttpStatus status) {
    return ResponseEntity.status(status).body(BaseResponse.of(status, null));
  }
}
