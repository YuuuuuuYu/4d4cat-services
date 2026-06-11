package com.services.api.applydays.dto;

import java.util.List;
import java.util.UUID;

public record BulkRejectRequest(
    List<UUID> requestIds, String reason, List<RejectionDetail> details) {}
