package com.services.api.applydays.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BulkApproveRequest(List<UUID> requestIds, String newSlug, Instant scheduledAt) {}
