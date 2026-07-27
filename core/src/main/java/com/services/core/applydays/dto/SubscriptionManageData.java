package com.services.core.applydays.dto;

import com.services.core.applydays.entity.subscription.ApplyDaysPayment;
import com.services.core.applydays.entity.subscription.ApplyDaysPaymentMethod;
import com.services.core.applydays.entity.subscription.ApplyDaysSubscription;
import java.util.List;
import java.util.Optional;

public record SubscriptionManageData(
    Optional<ApplyDaysSubscription> subscription,
    Optional<ApplyDaysPaymentMethod> paymentMethod,
    List<ApplyDaysPayment> paymentHistories,
    boolean hasBillingKey) {}
