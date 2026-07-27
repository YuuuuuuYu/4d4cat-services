package com.services.api.applydays.controller;

import com.services.api.applydays.dto.PaySubscriptionRequest;
import com.services.api.applydays.dto.PaymentHistoryResponse;
import com.services.api.applydays.dto.PaymentMethodResponse;
import com.services.api.applydays.dto.PortOneWebhookRequest;
import com.services.api.applydays.dto.PreRegisterRequest;
import com.services.api.applydays.dto.SubscriptionManageInfoResponse;
import com.services.api.applydays.dto.SubscriptionPlanResponse;
import com.services.api.applydays.dto.SubscriptionResponse;
import com.services.core.applydays.dto.SubscriptionManageData;
import com.services.core.applydays.entity.subscription.ApplyDaysSubscription;
import com.services.core.applydays.service.ApplyDaysPaymentMethodQueryService;
import com.services.core.applydays.service.ApplyDaysSubscriptionCommandService;
import com.services.core.applydays.service.ApplyDaysSubscriptionQueryService;
import com.services.core.common.dto.BaseResponse;
import com.services.core.common.exception.ErrorCode;
import com.services.core.common.exception.NotFoundException;
import com.services.core.common.exception.UnauthorizedException;
import com.services.core.common.persistence.entity.member.Member;
import com.services.core.common.persistence.repository.member.MemberRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

  @PostMapping("/pre-register")
  public BaseResponse<SubscriptionResponse> preRegister(
      Authentication authentication, @RequestBody PreRegisterRequest request) {

    if (authentication == null) {
      throw new UnauthorizedException(ErrorCode.UNAUTHORIZED);
    }
    String email = authentication.getName();
    log.info("Subscription pre-registration requested for user: {}", email);

    Member member =
        memberRepository
            .findByEmail(email)
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

    ApplyDaysSubscription subscription =
        subscriptionService.preRegister(member.getId(), request.planId());

    return BaseResponse.of(HttpStatus.CREATED, SubscriptionResponse.from(subscription));
  }

  @PostMapping("/pay")
  public BaseResponse<SubscriptionResponse> paySubscription(
      Authentication authentication, @RequestBody PaySubscriptionRequest request) {

    if (authentication == null) {
      throw new UnauthorizedException(ErrorCode.UNAUTHORIZED);
    }
    String email = authentication.getName();
    log.info(
        "Payment subscription request received: email={}, billingKey={}, paymentId={}",
        email,
        request.billingKey(),
        request.paymentId());

    Member member =
        memberRepository
            .findByEmail(email)
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

    ApplyDaysSubscription subscription =
        subscriptionService.processPayment(
            member.getId(), request.billingKey(), request.paymentId());

    return BaseResponse.of(HttpStatus.CREATED, SubscriptionResponse.from(subscription));
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

    return BaseResponse.of(HttpStatus.OK, SubscriptionResponse.from(subscription));
  }

  @DeleteMapping("/card")
  public BaseResponse<Void> deleteCard(Authentication authentication) {
    if (authentication == null) {
      throw new UnauthorizedException(ErrorCode.UNAUTHORIZED);
    }
    String email = authentication.getName();
    log.info("Subscription card delete request received for user: {}", email);

    Member member =
        memberRepository
            .findByEmail(email)
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

    subscriptionService.deleteCard(member.getId());

    return BaseResponse.of(HttpStatus.OK, null);
  }

  @PostMapping("/webhook")
  public BaseResponse<Void> handlePortOneWebhook(@RequestBody PortOneWebhookRequest request) {
    log.info(
        "PortOne V2 Webhook received: type={}, timestamp={}", request.type(), request.timestamp());

    if ("Transaction.Paid".equalsIgnoreCase(request.type()) && request.data() != null) {
      String paymentId = request.data().paymentId();
      UUID memberId = extractMemberIdFromPaymentId(paymentId);
      if (memberId != null) {
        subscriptionService.processPayment(memberId, null, paymentId);
        log.info("PortOne V2 Webhook processed successfully for memberId={}", memberId);
      } else {
        log.warn("Could not extract memberId from paymentId: {}", paymentId);
        return BaseResponse.of(HttpStatus.BAD_REQUEST, null);
      }
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
