package com.services.api.applydays.dto;

public record MySummaryResponse(
    long totalCount,
    long pendingCount,
    long rejectedCount,
    long approvedCount,
    SubscriptionResponse subscription) {}
