package com.services.api.applydays.dto;

import com.services.core.applydays.entity.subscription.ApplyDaysSubscription;
import com.services.core.applydays.entity.subscription.SubscriptionStatus;
import java.time.LocalDate;
import java.util.UUID;

public record SubscriptionResponse(
    UUID id,
    UUID memberId,
    UUID planId,
    SubscriptionStatus status,
    LocalDate startDate,
    LocalDate endDate,
    LocalDate nextBillingDate,
    boolean hasBillingKey,
    String accessToken) {

  public static SubscriptionResponse from(
      ApplyDaysSubscription subscription, boolean hasBillingKey, String accessToken) {
    return new SubscriptionResponse(
        subscription.getId(),
        subscription.getMemberId(),
        subscription.getPlanId(),
        subscription.getStatus(),
        subscription.getStartDate(),
        subscription.getEndDate(),
        subscription.getNextBillingDate(),
        hasBillingKey,
        accessToken);
  }

  public static SubscriptionResponse from(
      ApplyDaysSubscription subscription, boolean hasBillingKey) {
    return from(subscription, hasBillingKey, null);
  }

  public static SubscriptionResponse from(ApplyDaysSubscription subscription) {
    return from(subscription, false, null);
  }
}
