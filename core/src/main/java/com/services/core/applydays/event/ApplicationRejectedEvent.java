package com.services.core.applydays.event;

import java.util.UUID;

public record ApplicationRejectedEvent(
    UUID requestId, UUID memberId, UUID applicationId, String reason) {}
