package com.services.core.applydays.event;

import java.time.Instant;
import java.util.UUID;

public record ApplicationApprovedEvent(
    UUID requestId, UUID memberId, UUID applicationId, String newSlug, Instant scheduledAt) {}
