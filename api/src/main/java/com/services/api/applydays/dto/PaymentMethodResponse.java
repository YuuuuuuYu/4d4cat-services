package com.services.api.applydays.dto;

import com.services.core.applydays.entity.subscription.ApplyDaysPaymentMethod;
import java.util.UUID;

public record PaymentMethodResponse(
    UUID id,
    UUID memberId,
    String billingKey,
    String cardCompany,
    String cardNumberMasked,
    boolean isDefault) {

  public static PaymentMethodResponse from(ApplyDaysPaymentMethod pm) {
    return new PaymentMethodResponse(
        pm.getId(),
        pm.getMemberId(),
        pm.getBillingKey(),
        pm.getCardCompany(),
        pm.getCardNumberMasked(),
        pm.isDefault());
  }
}
