package com.services.core.applydays.event;

import java.time.LocalDate;
import java.util.UUID;

public record SubscriptionResumedEvent(
    UUID memberId, String memberName, String memberEmail, LocalDate nextBillingDate) {}
