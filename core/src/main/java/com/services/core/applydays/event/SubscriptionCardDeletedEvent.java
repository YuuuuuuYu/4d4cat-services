package com.services.core.applydays.event;

import java.util.UUID;

public record SubscriptionCardDeletedEvent(UUID memberId, String memberName, String memberEmail) {}
