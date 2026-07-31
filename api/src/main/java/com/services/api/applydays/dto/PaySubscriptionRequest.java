package com.services.api.applydays.dto;

import java.util.UUID;

public record PaySubscriptionRequest(String billingKey, String paymentId, UUID planId) {}
