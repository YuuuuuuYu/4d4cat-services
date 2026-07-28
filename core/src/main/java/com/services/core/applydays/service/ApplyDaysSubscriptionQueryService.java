package com.services.core.applydays.service;

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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplyDaysSubscriptionQueryService {

  private final ApplyDaysSubscriptionRepository subscriptionRepository;
  private final ApplyDaysSubscriptionPlanRepository planRepository;
  private final ApplyDaysPaymentRepository paymentRepository;
  private final ApplyDaysPaymentMethodRepository paymentMethodRepository;

  public List<ApplyDaysSubscriptionPlan> getActivePlans() {
    return planRepository.findAllByDeletedFalse();
  }

  public Optional<ApplyDaysSubscription> getSubscriptionByMemberId(UUID memberId) {
    return subscriptionRepository.findByMemberId(memberId);
  }

  public List<ApplyDaysPayment> getPaymentHistory(UUID memberId) {
    return paymentRepository.findAllByMemberIdOrderByCreatedAtDesc(memberId);
  }

  public SubscriptionManageData getSubscriptionManageInfoData(UUID memberId) {
    Optional<ApplyDaysSubscription> subscription = subscriptionRepository.findByMemberId(memberId);
    Optional<ApplyDaysPaymentMethod> paymentMethod =
        paymentMethodRepository.findByMemberIdAndDeletedFalse(memberId);
    List<ApplyDaysPayment> paymentHistories =
        paymentRepository.findAllByMemberIdOrderByCreatedAtDesc(memberId);

    return new SubscriptionManageData(
        subscription, paymentMethod, paymentHistories, paymentMethod.isPresent());
  }

  public Optional<ApplyDaysPayment> getPaymentByPortonePaymentId(String paymentId) {
    return paymentRepository.findByPortonePaymentId(paymentId);
  }

  public Optional<ApplyDaysSubscriptionPlan> getPlan(UUID planId) {
    return planRepository.findById(planId);
  }
}
