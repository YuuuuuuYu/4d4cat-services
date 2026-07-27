package com.services.core.applydays.service;

import com.services.core.applydays.entity.VerificationStatus;
import com.services.core.applydays.entity.subscription.ApplyDaysPayment;
import com.services.core.applydays.entity.subscription.ApplyDaysPaymentMethod;
import com.services.core.applydays.entity.subscription.ApplyDaysSubscription;
import com.services.core.applydays.entity.subscription.ApplyDaysSubscriptionPlan;
import com.services.core.applydays.entity.subscription.PaymentStatus;
import com.services.core.applydays.entity.subscription.SubscriptionStatus;
import com.services.core.applydays.event.SubscriptionCanceledEvent;
import com.services.core.applydays.event.SubscriptionCardDeletedEvent;
import com.services.core.applydays.event.SubscriptionExpiredEvent;
import com.services.core.applydays.event.SubscriptionPaidEvent;
import com.services.core.applydays.event.SubscriptionPaymentFailedEvent;
import com.services.core.applydays.event.SubscriptionResumedEvent;
import com.services.core.applydays.repository.ApplyDaysPaymentRepository;
import com.services.core.applydays.repository.ApplyDaysSubscriptionPlanRepository;
import com.services.core.applydays.repository.ApplyDaysSubscriptionRepository;
import com.services.core.applydays.repository.VerificationRequestRepository;
import com.services.core.common.exception.BadRequestException;
import com.services.core.common.exception.ErrorCode;
import com.services.core.common.exception.NotFoundException;
import com.services.core.common.infrastructure.external.portone.PortOneClient;
import com.services.core.common.infrastructure.external.portone.PortOneClient.PortOnePaymentResponse;
import com.services.core.common.persistence.entity.member.Member;
import com.services.core.common.persistence.entity.member.Role;
import com.services.core.common.persistence.repository.member.MemberRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplyDaysSubscriptionCommandService {

  private final ApplyDaysSubscriptionRepository subscriptionRepository;
  private final ApplyDaysSubscriptionPlanRepository planRepository;
  private final ApplyDaysPaymentRepository paymentRepository;
  private final VerificationRequestRepository verificationRequestRepository;
  private final MemberRepository memberRepository;
  private final PortOneClient portOneClient;
  private final ApplyDaysPaymentMethodQueryService paymentMethodQueryService;
  private final ApplyDaysPaymentMethodCommandService paymentMethodCommandService;
  private final ObjectProvider<ApplyDaysSubscriptionCommandService> selfProvider;
  private final MeterRegistry meterRegistry;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  public ApplyDaysSubscription preRegister(UUID memberId, UUID planId) {
    planRepository
        .findById(planId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.SUBSCRIPTION_PLAN_NOT_FOUND));

    Optional<ApplyDaysSubscription> existing = subscriptionRepository.findByMemberId(memberId);
    if (existing.isPresent()) {
      ApplyDaysSubscription s = existing.get();
      if (s.getStatus() == SubscriptionStatus.ACTIVE) {
        throw new BadRequestException(ErrorCode.ALREADY_SUBSCRIBED);
      }
      if (s.getStatus() == SubscriptionStatus.CANCELED) {
        s.preRegister(planId);
        ApplyDaysSubscription saved = subscriptionRepository.save(s);
        meterRegistry.counter("applydays.subscriptions.preregistered").increment();
        return saved;
      }
      subscriptionRepository.delete(s);
      subscriptionRepository.flush();
    }

    ApplyDaysSubscription subscription =
        ApplyDaysSubscription.builder()
            .memberId(memberId)
            .planId(planId)
            .status(SubscriptionStatus.PRE_REGISTERED)
            .build();

    ApplyDaysSubscription saved = subscriptionRepository.save(subscription);
    meterRegistry.counter("applydays.subscriptions.preregistered").increment();
    return saved;
  }

  /**
   * 최초 결제 요청 및 검증 (Transaction-After-API 패턴 적용) 외부 API 호출은 트랜잭션 외부에서 수행, 이후 DB 락 및 처리를 트랜잭션 내에서 처리
   */
  @Transactional
  public void scheduleCancellationOnCardDeleted(UUID memberId) {
    log.info("Scheduling cancellation on card deleted for memberId={}", memberId);
    subscriptionRepository
        .findByMemberId(memberId)
        .ifPresent(
            subscription -> {
              if (subscription.getStatus() == SubscriptionStatus.ACTIVE) {
                subscription.cancel();
                subscriptionRepository.save(subscription);
                log.info(
                    "Subscription scheduled for cancellation due to payment method deletion for memberId={}",
                    memberId);
              }
            });
  }

  /**
   * 최초 결제 요청 및 검증 (Transaction-After-API 패턴 적용) 외부 API 호출은 트랜잭션 외부에서 수행, 이후 DB 락 및 처리를 트랜잭션 내에서 처리
   */
  public ApplyDaysSubscription processPayment(UUID memberId, String billingKey, String paymentId) {
    log.info(
        "Processing subscription payment outside transaction: memberId={}, billingKey={}, paymentId={}",
        memberId,
        billingKey,
        paymentId);

    // 1. Idempotency Check
    if (paymentRepository.existsByPortonePaymentId(paymentId)) {
      log.warn("Payment with paymentId={} already processed. Skipping.", paymentId);
      return subscriptionRepository
          .findByMemberId(memberId)
          .orElseThrow(() -> new NotFoundException(ErrorCode.SUBSCRIPTION_NOT_FOUND));
    }

    if (billingKey != null && !billingKey.isBlank()) {
      paymentMethodCommandService.registerPaymentMethod(memberId, billingKey, null, null, true);
    }

    String effectiveBillingKey =
        (billingKey != null && !billingKey.isBlank())
            ? billingKey
            : paymentMethodQueryService
                .findByMemberId(memberId)
                .map(ApplyDaysPaymentMethod::getBillingKey)
                .orElse(null);

    // 2. 외부 API를 통해 결제 승인 또는 검증 수행 (Connection Pool 점유 예방)
    PortOnePaymentResponse portoneResponse;
    if (effectiveBillingKey != null && !effectiveBillingKey.isBlank()) {
      ApplyDaysSubscription subscription =
          subscriptionRepository
              .findByMemberId(memberId)
              .orElseThrow(() -> new NotFoundException(ErrorCode.SUBSCRIPTION_NOT_FOUND));
      ApplyDaysSubscriptionPlan plan =
          planRepository
              .findById(subscription.getPlanId())
              .orElseThrow(() -> new NotFoundException(ErrorCode.SUBSCRIPTION_PLAN_NOT_FOUND));

      try {
        portoneResponse =
            portOneClient.payWithBillingKey(
                effectiveBillingKey, paymentId, plan.getPrice(), plan.getName());
      } catch (Exception e) {
        log.error(
            "PortOne billing-key payment call failed outside transaction for memberId={}",
            memberId,
            e);
        selfProvider
            .getObject()
            .saveFailedPaymentAttempt(memberId, paymentId, "Payment request call failed");
        throw new BadRequestException(ErrorCode.PAYMENT_FAILED);
      }
    } else {
      try {
        portoneResponse = portOneClient.verifyPayment(paymentId);
      } catch (Exception e) {
        log.error(
            "PortOne payment verification call failed outside transaction for memberId={}",
            memberId,
            e);
        selfProvider
            .getObject()
            .saveFailedPaymentAttempt(memberId, paymentId, "Payment verification call failed");
        throw new BadRequestException(ErrorCode.PAYMENT_FAILED);
      }
    }

    // 3. 결제 결과를 바탕으로 DB 트랜잭션 처리 위임 (Self-Proxy 호출)
    return selfProvider.getObject().savePaymentAndActivate(memberId, paymentId, portoneResponse);
  }

  @Transactional
  public ApplyDaysSubscription savePaymentAndActivate(
      UUID memberId, String paymentId, PortOnePaymentResponse portoneResponse) {
    // Lock subscription
    ApplyDaysSubscription subscription =
        subscriptionRepository
            .findWithLockByMemberId(memberId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

    // Double check idempotency under lock
    if (paymentRepository.existsByPortonePaymentId(paymentId)) {
      return subscription;
    }

    ApplyDaysSubscriptionPlan plan =
        planRepository
            .findById(subscription.getPlanId())
            .orElseThrow(() -> new NotFoundException(ErrorCode.SUBSCRIPTION_PLAN_NOT_FOUND));

    // Validate payment status & amount
    if (!"PAID".equalsIgnoreCase(portoneResponse.status())) {
      log.error("Payment status is not PAID: {}", portoneResponse.status());
      String failReason =
          portoneResponse.failed() != null ? portoneResponse.failed().reason() : "Unknown error";
      savePayment(
          subscription,
          memberId,
          plan.getPrice(),
          PaymentStatus.FAILED,
          paymentId,
          failReason,
          null,
          portoneResponse.pgTxId());
      subscription.markPaymentFailed();
      subscriptionRepository.save(subscription);
      throw new BadRequestException(ErrorCode.PAYMENT_FAILED);
    }

    long actualAmount = portoneResponse.amount() != null ? portoneResponse.amount().total() : 0L;
    if (actualAmount != plan.getPrice()) {
      log.error("Payment amount mismatch: expected={}, actual={}", plan.getPrice(), actualAmount);
      String errorMsg = "Amount mismatch: expected=" + plan.getPrice() + ", actual=" + actualAmount;
      savePayment(
          subscription,
          memberId,
          plan.getPrice(),
          PaymentStatus.FAILED,
          paymentId,
          errorMsg,
          null,
          portoneResponse.pgTxId());
      subscription.markPaymentFailed();
      subscriptionRepository.save(subscription);
      throw new BadRequestException(ErrorCode.PAYMENT_FAILED);
    }

    // Activate Subscription
    LocalDate today = LocalDate.now();
    LocalDate endDate;
    LocalDate nextBillingDate;
    LocalDate startDate;

    if (subscription.getEndDate() != null && !subscription.getEndDate().isBefore(today)) {
      endDate = subscription.getEndDate().plusMonths(plan.getBillingCycleMonths());
      nextBillingDate = endDate.plusDays(1);
      startDate = subscription.getStartDate() != null ? subscription.getStartDate() : today;
    } else {
      startDate = today;
      endDate = today.plusMonths(plan.getBillingCycleMonths()).minusDays(1);
      nextBillingDate = today.plusMonths(plan.getBillingCycleMonths());
    }

    subscription.activate(startDate, endDate, nextBillingDate);
    ApplyDaysSubscription savedSubscription = subscriptionRepository.save(subscription);

    // Record success payment
    String receipt = portoneResponse.receiptUrl();
    savePayment(
        savedSubscription,
        memberId,
        plan.getPrice(),
        PaymentStatus.SUCCESS,
        paymentId,
        null,
        receipt,
        portoneResponse.pgTxId());

    // Update Member Role
    Member member =
        memberRepository
            .findById(memberId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
    if (member.getRole() != Role.ADMIN) {
      member.updateRole(Role.SUBSCRIBER);
      memberRepository.save(member);
    }

    LocalDateTime now = LocalDateTime.now();
    eventPublisher.publishEvent(
        new SubscriptionPaidEvent(
            memberId,
            member.getName(),
            member.getEmail(),
            paymentId,
            plan.getName(),
            plan.getPrice(),
            now,
            nextBillingDate,
            receipt,
            false));

    meterRegistry.counter("applydays.subscriptions.activated").increment();
    return savedSubscription;
  }

  @Transactional
  public void saveFailedPaymentAttempt(UUID memberId, String paymentId, String failReason) {
    ApplyDaysSubscription subscription =
        subscriptionRepository.findWithLockByMemberId(memberId).orElse(null);
    if (subscription != null) {
      ApplyDaysSubscriptionPlan plan =
          planRepository.findById(subscription.getPlanId()).orElse(null);
      long price = plan != null ? plan.getPrice() : 0L;
      savePayment(
          subscription, memberId, price, PaymentStatus.FAILED, paymentId, failReason, null, null);
      subscription.markPaymentFailed();
      subscriptionRepository.save(subscription);

      Member member = memberRepository.findById(memberId).orElse(null);
      String email = member != null ? member.getEmail() : "Unknown";
      String name = member != null ? member.getName() : "Unknown";
      String planName = plan != null ? plan.getName() : null;

      eventPublisher.publishEvent(
          new SubscriptionPaymentFailedEvent(
              memberId, name, email, planName, paymentId, failReason));
    }
  }

  @Transactional
  public void cancelSubscription(UUID memberId) {
    log.info("Canceling subscription for memberId={}", memberId);
    ApplyDaysSubscription subscription =
        subscriptionRepository
            .findWithLockByMemberId(memberId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

    if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
      throw new BadRequestException(ErrorCode.SUBSCRIPTION_NOT_FOUND);
    }

    subscription.cancel();
    ApplyDaysSubscription saved = subscriptionRepository.save(subscription);

    ApplyDaysSubscriptionPlan plan =
        planRepository
            .findById(subscription.getPlanId())
            .orElseThrow(() -> new NotFoundException(ErrorCode.SUBSCRIPTION_PLAN_NOT_FOUND));

    Member member =
        memberRepository
            .findById(memberId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

    eventPublisher.publishEvent(
        new SubscriptionCanceledEvent(
            memberId, member.getName(), member.getEmail(), plan.getName(), saved.getEndDate()));

    meterRegistry.counter("applydays.subscriptions.canceled").increment();
  }

  @Transactional
  public ApplyDaysSubscription resumeSubscription(UUID memberId) {
    log.info("Resuming subscription for memberId={}", memberId);
    ApplyDaysSubscription subscription =
        subscriptionRepository
            .findWithLockByMemberId(memberId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

    if (subscription.getStatus() != SubscriptionStatus.CANCELED) {
      throw new BadRequestException(ErrorCode.INVALID_SUBSCRIPTION_STATUS);
    }

    if (subscription.getEndDate() == null || subscription.getEndDate().isBefore(LocalDate.now())) {
      throw new BadRequestException(ErrorCode.SUBSCRIPTION_EXPIRED);
    }

    if (!paymentMethodQueryService.hasPaymentMethod(memberId)) {
      throw new BadRequestException(ErrorCode.BILLING_KEY_NOT_FOUND);
    }

    subscription.resume();
    ApplyDaysSubscription saved = subscriptionRepository.save(subscription);

    Member member =
        memberRepository
            .findById(memberId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

    if (member.getRole() != Role.ADMIN) {
      member.updateRole(Role.SUBSCRIBER);
      memberRepository.save(member);
    }

    eventPublisher.publishEvent(
        new SubscriptionResumedEvent(
            memberId, member.getName(), member.getEmail(), saved.getNextBillingDate()));

    meterRegistry.counter("applydays.subscriptions.resumed").increment();
    return saved;
  }

  @Transactional
  public void deleteCard(UUID memberId) {
    log.info("Deleting card billing key for memberId={}", memberId);
    paymentMethodCommandService.deletePaymentMethod(memberId);

    Member member =
        memberRepository
            .findById(memberId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

    eventPublisher.publishEvent(
        new SubscriptionCardDeletedEvent(memberId, member.getName(), member.getEmail()));
  }

  public void processRenewal() {
    LocalDate today = LocalDate.now();
    int pageNumber = 0;
    int pageSize = 100;

    log.info("Starting subscription renewal batch at {}", today);

    while (true) {
      Pageable pageable = PageRequest.of(pageNumber, pageSize);
      Slice<ApplyDaysSubscription> targetSlice =
          subscriptionRepository.findAllByStatusAndNextBillingDateLessThanEqual(
              SubscriptionStatus.ACTIVE, today, pageable);

      List<ApplyDaysSubscription> targets = targetSlice.getContent();
      if (targets.isEmpty()) {
        break;
      }

      for (ApplyDaysSubscription sub : targets) {
        try {
          // Self-Proxy를 사용하여 개별 건을 격리된 트랜잭션(Requires New)으로 안전하게 실행
          selfProvider.getObject().renewSubscription(sub);
        } catch (Exception e) {
          log.error(
              "Failed to process renewal for subscription of member {}", sub.getMemberId(), e);
        }
      }

      if (!targetSlice.hasNext()) {
        break;
      }
      pageNumber++;
    }
  }

  public void processExpiration() {
    LocalDate today = LocalDate.now();
    int pageNumber = 0;
    int pageSize = 100;

    log.info("Starting subscription expiration batch at {}", today);

    while (true) {
      Pageable pageable = PageRequest.of(pageNumber, pageSize);
      Slice<ApplyDaysSubscription> targetSlice =
          subscriptionRepository.findAllByStatusAndEndDateLessThan(
              SubscriptionStatus.CANCELED, today, pageable);

      List<ApplyDaysSubscription> targets = targetSlice.getContent();
      if (targets.isEmpty()) {
        break;
      }

      for (ApplyDaysSubscription sub : targets) {
        try {
          // Self-Proxy를 사용하여 개별 건을 격리된 트랜잭션(Requires New)으로 안전하게 실행
          selfProvider.getObject().expireSubscriptionIsolated(sub.getMemberId());
        } catch (Exception e) {
          log.error(
              "Failed to process expiration for subscription of member {}", sub.getMemberId(), e);
        }
      }

      if (!targetSlice.hasNext()) {
        break;
      }
      pageNumber++;
    }
  }

  public void expireSubscription(ApplyDaysSubscription sub) {
    selfProvider.getObject().expireSubscriptionIsolated(sub.getMemberId());
  }

  /** 정기결제 갱신 처리 (Transaction-After-API 패턴 적용) 외부 API 호출은 트랜잭션 바깥에서 수행하고, 결과 기록만 격리된 트랜잭션 내에서 처리 */
  public void renewSubscription(ApplyDaysSubscription sub) {
    ApplyDaysSubscriptionPlan plan =
        planRepository
            .findById(sub.getPlanId())
            .orElseThrow(() -> new NotFoundException(ErrorCode.SUBSCRIPTION_PLAN_NOT_FOUND));

    String paymentId = "merchant_sub_" + UUID.randomUUID();
    PortOnePaymentResponse portoneResponse;

    String billingKey =
        paymentMethodQueryService
            .findByMemberId(sub.getMemberId())
            .map(ApplyDaysPaymentMethod::getBillingKey)
            .orElse(null);

    if (billingKey == null) {
      log.error("No payment method found for member {}", sub.getMemberId());
      selfProvider
          .getObject()
          .saveRenewalFailureIsolated(
              sub.getMemberId(), plan, paymentId, "No active payment method found");
      return;
    }

    try {
      portoneResponse =
          portOneClient.payWithBillingKey(billingKey, paymentId, plan.getPrice(), plan.getName());
    } catch (Exception e) {
      log.error(
          "PortOne payWithBillingKey call failed outside transaction for member {}",
          sub.getMemberId(),
          e);
      selfProvider
          .getObject()
          .saveRenewalFailureIsolated(
              sub.getMemberId(), plan, paymentId, "Billing key renewal call failed");
      return;
    }

    // DB 반영 작업을 격리된 트랜잭션에 위임
    selfProvider
        .getObject()
        .saveRenewalResultIsolated(sub.getMemberId(), portoneResponse, paymentId, plan);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void saveRenewalResultIsolated(
      UUID memberId,
      PortOnePaymentResponse portoneResponse,
      String paymentId,
      ApplyDaysSubscriptionPlan plan) {
    ApplyDaysSubscription subscription =
        subscriptionRepository
            .findWithLockByMemberId(memberId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

    if (subscription.getStatus() != SubscriptionStatus.ACTIVE
        || subscription.getNextBillingDate().isAfter(LocalDate.now())) {
      return; // Already processed
    }

    if (!"PAID".equalsIgnoreCase(portoneResponse.status())) {
      log.error(
          "PayWithBillingKey status is not PAID for subscription {}: {}",
          subscription.getId(),
          portoneResponse.status());
      String failReason =
          portoneResponse.failed() != null ? portoneResponse.failed().reason() : "Unknown error";
      saveRenewalFailureInternal(subscription, plan, paymentId, failReason);
      return;
    }

    // Success renewal
    LocalDate startDate = subscription.getNextBillingDate();
    LocalDate endDate = startDate.plusMonths(plan.getBillingCycleMonths()).minusDays(1);
    LocalDate nextBillingDate = startDate.plusMonths(plan.getBillingCycleMonths());

    subscription.renew(startDate, endDate, nextBillingDate);
    subscriptionRepository.save(subscription);

    String receiptUrl = portoneResponse.receiptUrl();
    savePayment(
        subscription,
        subscription.getMemberId(),
        plan.getPrice(),
        PaymentStatus.SUCCESS,
        paymentId,
        null,
        receiptUrl,
        portoneResponse.pgTxId());

    Member member =
        memberRepository
            .findById(subscription.getMemberId())
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

    eventPublisher.publishEvent(
        new SubscriptionPaidEvent(
            subscription.getMemberId(),
            member.getName(),
            member.getEmail(),
            paymentId,
            plan.getName(),
            plan.getPrice(),
            LocalDateTime.now(),
            nextBillingDate,
            receiptUrl,
            true));

    log.info("Successfully renewed subscription for memberId={}", subscription.getMemberId());
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void saveRenewalFailureIsolated(
      UUID memberId, ApplyDaysSubscriptionPlan plan, String paymentId, String failReason) {
    ApplyDaysSubscription subscription =
        subscriptionRepository
            .findWithLockByMemberId(memberId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.SUBSCRIPTION_NOT_FOUND));
    saveRenewalFailureInternal(subscription, plan, paymentId, failReason);
  }

  private void saveRenewalFailureInternal(
      ApplyDaysSubscription subscription,
      ApplyDaysSubscriptionPlan plan,
      String paymentId,
      String failReason) {
    subscription.markPaymentFailed();
    subscriptionRepository.save(subscription);

    savePayment(
        subscription,
        subscription.getMemberId(),
        plan.getPrice(),
        PaymentStatus.FAILED,
        paymentId,
        failReason,
        null,
        null);

    Member member =
        memberRepository
            .findById(subscription.getMemberId())
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

    demoteMemberRole(member);

    eventPublisher.publishEvent(
        new SubscriptionPaymentFailedEvent(
            subscription.getMemberId(),
            member.getName(),
            member.getEmail(),
            plan.getName(),
            paymentId,
            failReason));
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void expireSubscriptionIsolated(UUID memberId) {
    ApplyDaysSubscription subscription =
        subscriptionRepository
            .findWithLockByMemberId(memberId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

    if (subscription.getStatus() != SubscriptionStatus.CANCELED
        || !subscription.getEndDate().isBefore(LocalDate.now())) {
      return; // Already processed
    }

    subscription.expire();
    subscriptionRepository.save(subscription);

    ApplyDaysSubscriptionPlan plan =
        planRepository
            .findById(subscription.getPlanId())
            .orElseThrow(() -> new NotFoundException(ErrorCode.SUBSCRIPTION_PLAN_NOT_FOUND));

    Member member =
        memberRepository
            .findById(subscription.getMemberId())
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

    demoteMemberRole(member);

    eventPublisher.publishEvent(
        new SubscriptionExpiredEvent(
            subscription.getMemberId(),
            member.getName(),
            member.getEmail(),
            plan.getName(),
            subscription.getEndDate()));

    log.info("Expired subscription for memberId={}", subscription.getMemberId());
  }

  private void demoteMemberRole(Member member) {
    if (member.getRole() == Role.ADMIN) {
      return;
    }

    long approvedCount =
        verificationRequestRepository.countByMemberIdAndStatus(
            member.getId(), VerificationStatus.APPROVED);
    Role newRole = approvedCount > 0 ? Role.REVIEWER : Role.USER;
    member.updateRole(newRole);
    memberRepository.save(member);
    log.info("Demoted member {} to role {}", member.getId(), newRole);
  }

  private void savePayment(
      ApplyDaysSubscription subscription,
      UUID memberId,
      long price,
      PaymentStatus status,
      String paymentId,
      String failReason,
      String receiptUrl,
      String approvalNo) {
    String truncatedReason = failReason;
    if (truncatedReason != null && truncatedReason.length() > 255) {
      truncatedReason = truncatedReason.substring(0, 252) + "...";
    }

    ApplyDaysPayment payment =
        ApplyDaysPayment.builder()
            .subscriptionId(subscription.getId())
            .memberId(memberId)
            .amount(price)
            .status(status)
            .portonePaymentId(paymentId)
            .portoneMerchantUid(paymentId)
            .failReason(truncatedReason)
            .receiptUrl(receiptUrl)
            .approvalNo(approvalNo)
            .build();
    paymentRepository.save(payment);
  }
}
