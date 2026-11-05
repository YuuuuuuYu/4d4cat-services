package com.services.core.applydays.service;

import com.services.core.applydays.dto.SubscriptionPaymentId;
import com.services.core.applydays.entity.VerificationStatus;
import com.services.core.applydays.entity.subscription.ApplyDaysPayment;
import com.services.core.applydays.entity.subscription.ApplyDaysPaymentMethod;
import com.services.core.applydays.entity.subscription.ApplyDaysSubscription;
import com.services.core.applydays.entity.subscription.ApplyDaysSubscriptionPlan;
import com.services.core.applydays.entity.subscription.PaymentStatus;
import com.services.core.applydays.entity.subscription.SubscriptionStatus;
import com.services.core.applydays.event.SubscriptionCanceledEvent;
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
import org.springframework.util.StringUtils;

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

  private ApplyDaysSubscriptionPlan resolvePlan(UUID memberId, UUID planId) {
    if (planId != null) {
      return planRepository
          .findById(planId)
          .orElseThrow(() -> new NotFoundException(ErrorCode.SUBSCRIPTION_PLAN_NOT_FOUND));
    }
    Optional<ApplyDaysSubscription> existing = subscriptionRepository.findByMemberId(memberId);
    if (existing.isPresent()) {
      return planRepository
          .findById(existing.get().getPlanId())
          .orElseThrow(() -> new NotFoundException(ErrorCode.SUBSCRIPTION_PLAN_NOT_FOUND));
    }
    return planRepository.findAllByDeletedFalse().stream()
        .findFirst()
        .orElseThrow(() -> new NotFoundException(ErrorCode.SUBSCRIPTION_PLAN_NOT_FOUND));
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
                    "Subscription scheduled for cancellation due to payment method deletion for"
                        + " memberId={}",
                    memberId);
              }
            });
  }

  public ApplyDaysSubscription processPayment(UUID memberId, String billingKey, String paymentId) {
    return processPayment(memberId, billingKey, paymentId, null);
  }

  /**
   * 최초 결제 요청 및 검증 (Transaction-After-API 패턴 적용) 외부 API 호출은 트랜잭션 외부에서 수행, 이후 DB 락 및 처리를 트랜잭션 내에서 처리
   */
  public ApplyDaysSubscription processPayment(
      UUID memberId, String billingKey, String paymentId, UUID planId) {
    String attempt = StringUtils.hasText(paymentId) ? paymentId : UUID.randomUUID().toString();
    Charge charge =
        selfProvider.getObject().prepareCharge(memberId, billingKey, attempt, planId, null);
    if (charge.payment().getStatus() == PaymentStatus.SUCCESS) {
      return charge.subscription();
    }
    if (charge.payment().getStatus() == PaymentStatus.FAILED) {
      throw new BadRequestException(ErrorCode.PAYMENT_FAILED);
    }
    PortOnePaymentResponse result = requestPayment(charge);
    ApplyDaysSubscription subscription =
        selfProvider.getObject().settleCharge(charge.payment(), result);
    if ("FAILED".equalsIgnoreCase(result.status())) {
      throw new BadRequestException(ErrorCode.PAYMENT_FAILED);
    }
    return subscription;
  }

  public record Charge(
      ApplyDaysPayment payment,
      ApplyDaysSubscription subscription,
      ApplyDaysSubscriptionPlan plan,
      boolean created) {}

  @Transactional
  public Charge prepareCharge(
      UUID memberId, String billingKey, String attempt, UUID planId, LocalDate renewalDate) {
    memberRepository
        .findByIdForUpdate(memberId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
    var existingSubscription = subscriptionRepository.findWithLockByMemberId(memberId);
    boolean renewal = renewalDate != null;
    if (renewal
        && existingSubscription
            .filter(
                subscription ->
                    subscription.getStatus() == SubscriptionStatus.ACTIVE
                        && renewalDate.equals(subscription.getNextBillingDate())
                        && !renewalDate.isAfter(LocalDate.now()))
            .isEmpty()) {
      return null;
    }

    String paymentId =
        SubscriptionPaymentId.create(memberId, (renewal ? "renew:" : "pay:") + attempt);
    var matching = paymentRepository.findByPortonePaymentId(paymentId);
    if (matching.isPresent()) {
      ApplyDaysPayment payment = matching.get();
      return new Charge(
          payment,
          existingSubscription.orElseThrow(),
          resolvePlan(memberId, payment.getPaymentPlanId()),
          false);
    }
    var pending =
        paymentRepository.findFirstByMemberIdAndStatusOrderByCreatedAtAsc(
            memberId, PaymentStatus.PENDING);
    if (pending.isPresent()) {
      ApplyDaysPayment payment = pending.get();
      if (payment.isRenewal() != renewal) {
        throw new BadRequestException(ErrorCode.PAYMENT_PENDING);
      }
      return new Charge(
          payment,
          existingSubscription.orElseThrow(),
          resolvePlan(memberId, payment.getPaymentPlanId()),
          false);
    }

    ApplyDaysSubscriptionPlan plan = resolvePlan(memberId, planId);
    if (plan.isDeleted()) {
      throw new NotFoundException(ErrorCode.SUBSCRIPTION_PLAN_NOT_FOUND);
    }
    String key =
        StringUtils.hasText(billingKey)
            ? billingKey
            : paymentMethodQueryService
                .findByMemberId(memberId)
                .map(ApplyDaysPaymentMethod::getBillingKey)
                .orElse(null);
    if (!StringUtils.hasText(key)) {
      if (renewal) {
        saveRenewalFailureInternal(
            existingSubscription.orElseThrow(), plan, paymentId, "No active payment method found");
        return null;
      }
      throw new BadRequestException(ErrorCode.BILLING_KEY_NOT_FOUND);
    }

    ApplyDaysSubscription subscription =
        existingSubscription.orElseGet(
            () ->
                subscriptionRepository.save(
                    ApplyDaysSubscription.builder()
                        .memberId(memberId)
                        .planId(plan.getId())
                        .status(SubscriptionStatus.PAYMENT_FAILED)
                        .build()));
    ApplyDaysPayment payment =
        ApplyDaysPayment.builder()
            .subscriptionId(subscription.getId())
            .memberId(memberId)
            .amount(plan.getPrice())
            .status(PaymentStatus.PENDING)
            .portonePaymentId(paymentId)
            .portoneMerchantUid(paymentId)
            .build();
    payment.prepare(plan.getId(), key, renewal);
    return new Charge(paymentRepository.save(payment), subscription, plan, true);
  }

  private PortOnePaymentResponse requestPayment(Charge charge) {
    ApplyDaysPayment payment = charge.payment();
    try {
      if (!charge.created()) {
        try {
          return portOneClient.verifyPayment(payment.getPortonePaymentId());
        } catch (PortOneClient.PaymentNotFound e) {
          if (!selfProvider
              .getObject()
              .claimChargeRetry(payment.getMemberId(), payment.getPortonePaymentId())) {
            throw new BadRequestException(ErrorCode.PAYMENT_PENDING);
          }
        }
      }
      return portOneClient.payWithBillingKey(
          payment.getPendingBillingKey(),
          payment.getPortonePaymentId(),
          payment.getAmount(),
          charge.plan().getName());
    } catch (RuntimeException e) {
      log.warn("PortOne result is unknown for payment {}", payment.getPortonePaymentId());
      throw new BadRequestException(ErrorCode.PAYMENT_PENDING);
    }
  }

  @Transactional
  public boolean claimChargeRetry(UUID memberId, String paymentId) {
    if (memberRepository.findByIdForUpdate(memberId).isEmpty()) return false;
    return paymentRepository
        .findByPortonePaymentId(paymentId)
        .filter(
            payment ->
                payment.getMemberId().equals(memberId)
                    && payment.getStatus() == PaymentStatus.PENDING)
        .filter(
            payment ->
                payment.getChargeRequestedAt() != null
                    && payment.getChargeRequestedAt().isBefore(LocalDateTime.now().minusMinutes(1)))
        .map(
            payment -> {
              if (payment.isRenewal()
                  && subscriptionRepository
                      .findWithLockByMemberId(memberId)
                      .filter(subscription -> subscription.getStatus() == SubscriptionStatus.ACTIVE)
                      .isEmpty()) return false;
              payment.markChargeRequested();
              return true;
            })
        .orElse(false);
  }

  @Transactional
  public ApplyDaysSubscription settleCharge(ApplyDaysPayment order, PortOnePaymentResponse result) {
    UUID memberId = order.getMemberId();
    memberRepository
        .findByIdForUpdate(memberId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
    ApplyDaysSubscription subscription =
        subscriptionRepository
            .findWithLockByMemberId(memberId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.SUBSCRIPTION_NOT_FOUND));
    ApplyDaysPayment payment =
        paymentRepository
            .findByPortonePaymentId(order.getPortonePaymentId())
            .orElseThrow(() -> new NotFoundException(ErrorCode.SUBSCRIPTION_NOT_FOUND));
    if (payment.getStatus() == PaymentStatus.SUCCESS) {
      return subscription;
    }
    if (result == null || !payment.getPortonePaymentId().equals(result.id())) {
      throw new BadRequestException(ErrorCode.PAYMENT_PENDING);
    }
    if ("FAILED".equalsIgnoreCase(result.status())) {
      if (payment.getStatus() == PaymentStatus.PENDING) {
        String reason = result.failed() == null ? "Payment failed" : result.failed().reason();
        if (payment.isRenewal() && subscription.getStatus() == SubscriptionStatus.ACTIVE) {
          saveRenewalFailureInternal(
              subscription,
              resolvePlan(memberId, payment.getPaymentPlanId()),
              payment.getPortonePaymentId(),
              reason);
        } else {
          payment.resolve(PaymentStatus.FAILED, reason, null, result.pgTxId());
        }
      }
      return subscription;
    }
    if (!"PAID".equalsIgnoreCase(result.status())
        || result.amount() == null
        || result.amount().total() != payment.getAmount()
        || !"KRW".equals(result.currency())) {
      throw new BadRequestException(ErrorCode.PAYMENT_PENDING);
    }
    if (payment.isRenewal()) {
      boolean canceled = subscription.getStatus() == SubscriptionStatus.CANCELED;
      if (canceled) {
        subscription.resume();
      }
      saveRenewalResultIsolated(
          memberId,
          result,
          payment.getPortonePaymentId(),
          resolvePlan(memberId, payment.getPaymentPlanId()));
      if (canceled) {
        subscription.cancel();
      }
      return subscription;
    }
    return savePaymentAndActivate(
        memberId,
        payment.getPendingBillingKey(),
        payment.getPortonePaymentId(),
        result,
        payment.getPaymentPlanId());
  }

  @Transactional
  public ApplyDaysSubscription savePaymentAndActivate(
      UUID memberId,
      String billingKey,
      String paymentId,
      PortOnePaymentResponse portoneResponse,
      UUID planId) {
    // Lock or create subscription
    Optional<ApplyDaysSubscription> subscriptionOpt =
        subscriptionRepository.findWithLockByMemberId(memberId);

    ApplyDaysSubscriptionPlan plan = resolvePlan(memberId, planId);

    ApplyDaysSubscription subscription;
    if (subscriptionOpt.isPresent()) {
      subscription = subscriptionOpt.get();
    } else {
      subscription =
          ApplyDaysSubscription.builder()
              .memberId(memberId)
              .planId(plan.getId())
              .status(SubscriptionStatus.PAYMENT_FAILED)
              .build();
    }

    // Double check idempotency under lock
    if (paymentRepository
        .findByPortonePaymentId(paymentId)
        .filter(payment -> payment.getStatus() == PaymentStatus.SUCCESS)
        .isPresent()) {
      return subscription;
    }

    // Validate payment status & amount
    if (portoneResponse == null || !"PAID".equalsIgnoreCase(portoneResponse.status())) {
      log.warn("Payment verification did not return PAID");
      String failReason =
          portoneResponse != null && portoneResponse.failed() != null
              ? portoneResponse.failed().reason()
              : "Payment verification failed";
      savePayment(
          subscription,
          memberId,
          plan.getPrice(),
          PaymentStatus.FAILED,
          paymentId,
          failReason,
          null,
          portoneResponse != null ? portoneResponse.pgTxId() : null);
      subscription.markPaymentFailed();
      subscriptionRepository.save(subscription);
      throw new BadRequestException(ErrorCode.PAYMENT_FAILED);
    }

    long expectedAmount =
        paymentRepository
            .findByPortonePaymentId(paymentId)
            .map(ApplyDaysPayment::getAmount)
            .orElse(plan.getPrice());
    long actualAmount = portoneResponse.amount() != null ? portoneResponse.amount().total() : 0L;
    if (actualAmount != expectedAmount
        || !"KRW".equals(portoneResponse.currency())
        || !paymentId.equals(portoneResponse.id())) {
      log.error("Payment amount mismatch: expected={}, actual={}", expectedAmount, actualAmount);
      String errorMsg = "Payment ID, currency or amount mismatch";
      savePayment(
          subscription,
          memberId,
          plan.getPrice(),
          PaymentStatus.FAILED,
          paymentId,
          errorMsg,
          null,
          portoneResponse != null ? portoneResponse.pgTxId() : null);
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

    if (billingKey != null && !billingKey.isBlank()) {
      paymentMethodCommandService.registerPaymentMethod(memberId, billingKey, null, null);
    }

    // Record success payment
    String receipt = portoneResponse.receiptUrl();
    savePayment(
        savedSubscription,
        memberId,
        actualAmount,
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
            actualAmount,
            now,
            nextBillingDate,
            receipt,
            false));

    meterRegistry.counter("applydays.subscriptions.activated").increment();
    return savedSubscription;
  }

  @Transactional
  public void cancelSubscription(UUID memberId) {
    log.info("Canceling subscription for memberId={}", memberId);
    ApplyDaysSubscription subscription =
        subscriptionRepository
            .findWithLockByMemberId(memberId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

    if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
      throw new BadRequestException(ErrorCode.INVALID_SUBSCRIPTION_STATUS);
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
    memberRepository.findByIdForUpdate(memberId);
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

  public int processRenewal() {
    LocalDate today = LocalDate.now();
    UUID cursor = new UUID(0, 0);
    int pageSize = 100;
    int totalProcessed = 0;

    log.info("Starting subscription renewal batch at {}", today);

    while (true) {
      Pageable pageable = PageRequest.of(0, pageSize);
      Slice<ApplyDaysSubscription> targetSlice =
          subscriptionRepository.findRenewals(today, cursor, pageable);

      List<ApplyDaysSubscription> targets = targetSlice.getContent();
      if (targets.isEmpty()) {
        break;
      }

      for (ApplyDaysSubscription sub : targets) {
        try {
          selfProvider.getObject().renewSubscription(sub);
          totalProcessed++;
        } catch (Exception e) {
          log.error(
              "Failed to process renewal for subscription of member {}", sub.getMemberId(), e);
        }
      }

      if (!targetSlice.hasNext()) {
        break;
      }
      cursor = targets.getLast().getMemberId();
    }
    return totalProcessed;
  }

  public void reconcilePendingPayments() {
    UUID cursor = new UUID(0, 0);
    Slice<ApplyDaysPayment> page;
    do {
      page =
          paymentRepository.findByStatusAndIdGreaterThanOrderById(
              PaymentStatus.PENDING, cursor, PageRequest.of(0, 100));
      for (ApplyDaysPayment payment : page.getContent()) {
        cursor = payment.getId();
        try {
          PortOnePaymentResponse result =
              portOneClient.verifyPayment(payment.getPortonePaymentId());
          selfProvider.getObject().settleCharge(payment, result);
        } catch (PortOneClient.PaymentNotFound e) {
          selfProvider.getObject().expireMissingCharge(payment);
        } catch (RuntimeException e) {
          log.warn("Payment {} still requires reconciliation", payment.getPortonePaymentId());
        }
      }
    } while (page.hasNext());
  }

  @Transactional
  public void expireMissingCharge(ApplyDaysPayment order) {
    UUID memberId = order.getMemberId();
    if (memberRepository.findByIdForUpdate(memberId).isEmpty()) return;
    ApplyDaysPayment payment =
        paymentRepository.findByPortonePaymentId(order.getPortonePaymentId()).orElse(null);
    if (payment == null
        || payment.getStatus() != PaymentStatus.PENDING
        || payment.getChargeRequestedAt() == null
        || payment.getChargeRequestedAt().isAfter(LocalDateTime.now().minusDays(1))) return;
    ApplyDaysSubscription subscription =
        subscriptionRepository.findWithLockByMemberId(memberId).orElseThrow();
    String reason = "No payment found at PortOne after 24 hours";
    if (payment.isRenewal() && subscription.getStatus() == SubscriptionStatus.ACTIVE) {
      saveRenewalFailureInternal(
          subscription,
          resolvePlan(memberId, payment.getPaymentPlanId()),
          payment.getPortonePaymentId(),
          reason);
    } else {
      payment.resolve(PaymentStatus.FAILED, reason, null, null);
    }
  }

  public int processExpiration() {
    LocalDate today = LocalDate.now();
    UUID cursor = new UUID(0, 0);
    int pageSize = 100;
    int totalProcessed = 0;

    log.info("Starting subscription expiration batch at {}", today);

    while (true) {
      Pageable pageable = PageRequest.of(0, pageSize);
      Slice<ApplyDaysSubscription> targetSlice =
          subscriptionRepository.findExpirations(today, cursor, pageable);

      List<ApplyDaysSubscription> targets = targetSlice.getContent();
      if (targets.isEmpty()) {
        break;
      }

      for (ApplyDaysSubscription sub : targets) {
        try {
          selfProvider.getObject().expireSubscriptionIsolated(sub.getMemberId());
          totalProcessed++;
        } catch (Exception e) {
          log.error(
              "Failed to process expiration for subscription of member {}", sub.getMemberId(), e);
        }
      }

      if (!targetSlice.hasNext()) {
        break;
      }
      cursor = targets.getLast().getMemberId();
    }
    return totalProcessed;
  }

  public void expireSubscription(ApplyDaysSubscription sub) {
    selfProvider.getObject().expireSubscriptionIsolated(sub.getMemberId());
  }

  /** 정기결제 갱신 처리 (Transaction-After-API 패턴 적용) 외부 API 호출은 트랜잭션 바깥에서 수행하고, 결과 기록만 격리된 트랜잭션 내에서 처리 */
  public void renewSubscription(ApplyDaysSubscription sub) {
    LocalDate due = sub.getNextBillingDate();
    if (due == null || memberRepository.findById(sub.getMemberId()).isEmpty()) {
      return;
    }
    Charge charge =
        selfProvider
            .getObject()
            .prepareCharge(sub.getMemberId(), null, due.toString(), sub.getPlanId(), due);
    if (charge == null || charge.payment().getStatus() != PaymentStatus.PENDING) {
      return;
    }
    PortOnePaymentResponse response = requestPayment(charge);
    selfProvider.getObject().settleCharge(charge.payment(), response);
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

    if (paymentRepository
        .findByPortonePaymentId(paymentId)
        .filter(payment -> payment.getStatus() == PaymentStatus.SUCCESS)
        .isPresent()) {
      return;
    }

    if (subscription.getStatus() != SubscriptionStatus.ACTIVE
        || subscription.getNextBillingDate() == null
        || subscription.getNextBillingDate().isAfter(LocalDate.now())) {
      return; // Already processed
    }

    if (portoneResponse == null || !"PAID".equalsIgnoreCase(portoneResponse.status())) {
      log.error(
          "PayWithBillingKey status is not PAID for subscription {}: {}",
          subscription.getId(),
          portoneResponse != null ? portoneResponse.status() : null);
      String failReason =
          portoneResponse != null && portoneResponse.failed() != null
              ? portoneResponse.failed().reason()
              : "Payment verification failed";
      saveRenewalFailureInternal(subscription, plan, paymentId, failReason);
      return;
    }

    long expectedAmount =
        paymentRepository
            .findByPortonePaymentId(paymentId)
            .map(ApplyDaysPayment::getAmount)
            .orElse(plan.getPrice());
    if (!paymentId.equals(portoneResponse.id())
        || portoneResponse.amount() == null
        || portoneResponse.amount().total() != expectedAmount
        || !"KRW".equals(portoneResponse.currency())) {
      saveRenewalFailureInternal(
          subscription, plan, paymentId, "Payment ID, currency or amount mismatch");
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
        expectedAmount,
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
            expectedAmount,
            LocalDateTime.now(),
            nextBillingDate,
            receiptUrl,
            true));

    log.info("Successfully renewed subscription for memberId={}", subscription.getMemberId());
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
    memberRepository.findByIdForUpdate(memberId);
    ApplyDaysSubscription subscription =
        subscriptionRepository
            .findWithLockByMemberId(memberId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

    if (subscription.getStatus() != SubscriptionStatus.CANCELED
        || !subscription.getEndDate().isBefore(LocalDate.now())
        || paymentRepository.existsByMemberIdAndStatus(memberId, PaymentStatus.PENDING)) {
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

    var existing = paymentRepository.findByPortonePaymentId(paymentId);
    if (existing.isPresent()) {
      ApplyDaysPayment payment = existing.get();
      payment.resolve(status, truncatedReason, receiptUrl, approvalNo);
      paymentRepository.save(payment);
      return;
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
