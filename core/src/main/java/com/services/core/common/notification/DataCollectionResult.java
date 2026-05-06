package com.services.core.common.notification;

public record DataCollectionResult(
    String taskName,
    int totalItems,
    long successFilters,
    long failedFilters,
    double durationSeconds) {}
