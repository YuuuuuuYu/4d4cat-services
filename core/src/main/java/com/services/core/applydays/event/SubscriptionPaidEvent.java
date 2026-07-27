package com.services.core.applydays.event;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record SubscriptionPaidEvent(
    UUID memberId,
    String memberName,
    String memberEmail,
    String paymentId,
    String planName,
    long price,
    LocalDateTime paidAt,
    LocalDate nextBillingDate,
    String receiptUrl,
    boolean isRenewal) {}
