package com.services.core.notification;

public record DataCollectionResult(
    String taskName,
    int totalItems,
    long successFilters,
    long failedFilters,
    double durationSeconds) {}
