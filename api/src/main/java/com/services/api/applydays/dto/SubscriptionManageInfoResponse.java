package com.services.api.applydays.dto;

import java.util.List;

public record SubscriptionManageInfoResponse(
    SubscriptionResponse subscription,
    PaymentMethodResponse paymentMethod,
    List<PaymentHistoryResponse> paymentHistories) {}
