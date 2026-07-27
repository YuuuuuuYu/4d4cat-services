package com.services.api.applydays.dto;

import com.services.core.applydays.entity.subscription.ApplyDaysSubscriptionPlan;
import java.util.UUID;

public record SubscriptionPlanResponse(
    UUID id, String name, Long price, Integer billingCycleMonths) {
  public static SubscriptionPlanResponse from(ApplyDaysSubscriptionPlan plan) {
    return new SubscriptionPlanResponse(
        plan.getId(), plan.getName(), plan.getPrice(), plan.getBillingCycleMonths());
  }
}
