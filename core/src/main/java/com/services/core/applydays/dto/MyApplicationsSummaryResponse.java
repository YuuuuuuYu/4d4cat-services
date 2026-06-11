package com.services.core.applydays.dto;

import lombok.Builder;

@Builder
public record MyApplicationsSummaryResponse(
    long totalCount, long pendingCount, long rejectedCount, long approvedCount) {}
