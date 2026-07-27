package com.services.core.applydays.service;

import com.services.core.applydays.entity.subscription.ApplyDaysPaymentMethod;
import com.services.core.applydays.event.PaymentMethodDeletedEvent;
import com.services.core.applydays.repository.ApplyDaysPaymentMethodRepository;
import com.services.core.common.exception.BadRequestException;
import com.services.core.common.exception.ErrorCode;
import com.services.core.common.infrastructure.external.portone.PortOneClient;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ApplyDaysPaymentMethodCommandService {

  private final ApplyDaysPaymentMethodRepository paymentMethodRepository;
  private final PortOneClient portOneClient;
  private final ApplicationEventPublisher eventPublisher;

  public ApplyDaysPaymentMethod registerPaymentMethod(
      UUID memberId,
      String billingKey,
      String cardCompany,
      String cardNumberMasked,
      boolean isDefault) {
    log.info("Registering payment method for memberId={}", memberId);

    Optional<ApplyDaysPaymentMethod> existingOpt = paymentMethodRepository.findByMemberId(memberId);

    ApplyDaysPaymentMethod paymentMethod;
    if (existingOpt.isPresent()) {
      paymentMethod = existingOpt.get();
      if (paymentMethod.isDeleted()) {
        paymentMethod.restore();
      }
      paymentMethod.updatePaymentMethod(billingKey, cardCompany, cardNumberMasked, isDefault);
    } else {
      paymentMethod =
          ApplyDaysPaymentMethod.builder()
              .memberId(memberId)
              .billingKey(billingKey)
              .cardCompany(cardCompany)
              .cardNumberMasked(cardNumberMasked)
              .isDefault(isDefault)
              .build();
    }

    return paymentMethodRepository.save(paymentMethod);
  }

  public void deletePaymentMethod(UUID memberId) {
    log.info("Deleting payment method for memberId={}", memberId);

    ApplyDaysPaymentMethod paymentMethod =
        paymentMethodRepository
            .findByMemberIdAndDeletedFalse(memberId)
            .orElseThrow(() -> new BadRequestException(ErrorCode.BILLING_KEY_NOT_FOUND));

    String billingKey = paymentMethod.getBillingKey();

    try {
      portOneClient.deleteBillingKey(billingKey);
    } catch (Exception e) {
      log.warn(
          "Failed to delete billing key via PortOneClient for memberId={}: {}",
          memberId,
          e.getMessage());
    }

    paymentMethod.delete();
    paymentMethodRepository.save(paymentMethod);

    eventPublisher.publishEvent(new PaymentMethodDeletedEvent(memberId, billingKey));
  }
}
