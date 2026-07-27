package com.services.core.applydays.event;

import java.util.UUID;

public record PaymentMethodDeletedEvent(UUID memberId, String billingKey) {}
