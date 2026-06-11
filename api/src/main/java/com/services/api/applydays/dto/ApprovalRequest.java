package com.services.api.applydays.dto;

import java.time.Instant;

public record ApprovalRequest(String newSlug, Instant scheduledAt) {}
