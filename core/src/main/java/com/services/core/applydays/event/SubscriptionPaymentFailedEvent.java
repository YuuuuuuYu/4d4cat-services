package com.services.core.applydays.event;

import java.util.UUID;

public record SubscriptionPaymentFailedEvent(
    UUID memberId,
    String memberName,
    String memberEmail,
    String planName,
    String paymentId,
    String failReason) {}
