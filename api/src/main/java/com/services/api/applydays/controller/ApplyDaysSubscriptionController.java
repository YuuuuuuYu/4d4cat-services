package com.services.api.applydays.controller;

import com.services.api.applydays.dto.PaySubscriptionRequest;
import com.services.api.applydays.dto.PaymentHistoryResponse;
import com.services.api.applydays.dto.PaymentMethodResponse;
import com.services.api.applydays.dto.PortOneWebhookRequest;
import com.services.api.applydays.dto.SubscriptionManageInfoResponse;
import com.services.api.applydays.dto.SubscriptionPlanResponse;
import com.services.api.applydays.dto.SubscriptionResponse;
import com.services.api.common.security.jwt.JwtProvider;
import com.services.core.applydays.dto.SubscriptionManageData;
import com.services.core.applydays.entity.subscription.ApplyDaysPayment;
import com.services.core.applydays.entity.subscription.ApplyDaysSubscription;
import com.services.core.applydays.entity.subscription.ApplyDaysSubscriptionPlan;
import com.services.core.applydays.entity.subscription.PaymentStatus;
import com.services.core.applydays.service.ApplyDaysPaymentMethodQueryService;
import com.services.core.applydays.service.ApplyDaysSubscriptionCommandService;
import com.services.core.applydays.service.ApplyDaysSubscriptionQueryService;
import com.services.core.common.dto.BaseResponse;
import com.services.core.common.exception.ErrorCode;
import com.services.core.common.exception.NotFoundException;
import com.services.core.common.exception.UnauthorizedException;
import com.services.core.common.infrastructure.external.portone.PortOneClient;
import com.services.core.common.infrastructure.external.portone.PortOneClient.PortOnePaymentResponse;
import com.services.core.common.persistence.entity.member.Member;
import com.services.core.common.persistence.repository.member.MemberRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
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
  public BaseResponse<Void> handlePortOneWebhook(
      @RequestHeader(value = "webhook-signature", required = false) String webhookSignature,
      @RequestHeader(value = "x-portone-signature", required = false) String xPortoneSignature,
      @RequestBody PortOneWebhookRequest request) {
    log.info(
        "PortOne V2 Webhook received: type={}, timestamp={}", request.type(), request.timestamp());

    if (StringUtils.hasText(webhookSecret)) {
      String signature =
          StringUtils.hasText(webhookSignature) ? webhookSignature : xPortoneSignature;
      if (!StringUtils.hasText(signature)
          || !MessageDigest.isEqual(
              webhookSecret.getBytes(StandardCharsets.UTF_8),
              signature.getBytes(StandardCharsets.UTF_8))) {
        log.warn("Invalid PortOne Webhook signature/secret");
        return BaseResponse.of(HttpStatus.UNAUTHORIZED, null);
      }
    }

    if ("Transaction.Paid".equalsIgnoreCase(request.type()) && request.data() != null) {
      String paymentId = request.data().paymentId();
      UUID memberId = extractMemberIdFromPaymentId(paymentId);
      if (memberId == null) {
        log.warn("Could not extract memberId from paymentId: {}", paymentId);
        return BaseResponse.of(HttpStatus.BAD_REQUEST, null);
      }

      ApplyDaysPayment existingPayment =
          subscriptionQueryService.getPaymentByPortonePaymentId(paymentId).orElse(null);
      if (existingPayment != null && existingPayment.getStatus() == PaymentStatus.SUCCESS) {
        log.info(
            "Webhook: Payment {} already processed successfully, idempotency check passed",
            paymentId);
        return BaseResponse.of(HttpStatus.OK, null);
      }

      try {
        PortOnePaymentResponse verifiedPayment = portOneClient.verifyPayment(paymentId);
        if (!"PAID".equalsIgnoreCase(verifiedPayment.status())) {
          log.warn("PortOne payment 2nd verification failed: status={}", verifiedPayment.status());
          return BaseResponse.of(HttpStatus.BAD_REQUEST, null);
        }

        ApplyDaysSubscription subscription =
            subscriptionQueryService.getSubscriptionByMemberId(memberId).orElse(null);
        if (subscription == null) {
          log.warn("Webhook: Subscription not found for memberId={}", memberId);
          return BaseResponse.of(HttpStatus.BAD_REQUEST, null);
        }

        ApplyDaysSubscriptionPlan plan =
            subscriptionQueryService.getPlan(subscription.getPlanId()).orElse(null);
        if (plan == null) {
          log.warn("Webhook: Subscription plan not found for planId={}", subscription.getPlanId());
          return BaseResponse.of(HttpStatus.BAD_REQUEST, null);
        }

        if (verifiedPayment.amount().total() != plan.getPrice()) {
          log.warn(
              "Webhook: Payment amount verification failed. expected={}, actual={}",
              plan.getPrice(),
              verifiedPayment.amount().total());
          return BaseResponse.of(HttpStatus.BAD_REQUEST, null);
        }

      } catch (Exception e) {
        log.error("PortOne payment 2nd verification exception for paymentId={}", paymentId, e);
        return BaseResponse.of(HttpStatus.BAD_REQUEST, null);
      }

      subscriptionService.processPayment(memberId, null, paymentId);
      log.info("PortOne V2 Webhook processed successfully for memberId={}", memberId);
    } else {
      log.info("Ignoring webhook type: {}", request.type());
    }

    return BaseResponse.of(HttpStatus.OK, null);
  }

  private UUID extractMemberIdFromPaymentId(String paymentId) {
    try {
      if (paymentId != null && paymentId.startsWith("sub_")) {
        String[] parts = paymentId.split("_");
        if (parts.length >= 2) {
          return UUID.fromString(parts[1]);
        }
      }
    } catch (Exception e) {
      log.error("Failed to parse memberId from paymentId: {}", paymentId, e);
    }
    return null;
  }
}
