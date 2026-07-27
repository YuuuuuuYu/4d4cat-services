package com.services.core.applydays.event;

import java.time.LocalDate;
import java.util.UUID;

public record SubscriptionCanceledEvent(
    UUID memberId, String memberName, String memberEmail, String planName, LocalDate endDate) {}
