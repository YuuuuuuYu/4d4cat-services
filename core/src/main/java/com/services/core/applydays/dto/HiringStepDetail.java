package com.services.core.applydays.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record HiringStepDetail(
    String stepType,
    String stepDate,
    String status,
    @JsonProperty("duration_days") Integer durationDays) {}
