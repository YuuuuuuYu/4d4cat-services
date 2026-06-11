package com.services.core.applydays.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.Getter;

@Getter
public class TimelineBasicResponse {
  private final UUID id;
  private final String categoryL1Name;
  private final String categoryL2Name;
  private final String appliedMonth;
  private final LocalDateTime createdAt;

  public TimelineBasicResponse(
      UUID id,
      String categoryL1Name,
      String categoryL2Name,
      LocalDateTime appliedAt,
      LocalDateTime createdAt) {
    this.id = id;
    this.categoryL1Name = categoryL1Name;
    this.categoryL2Name = categoryL2Name;
    this.appliedMonth =
        appliedAt != null ? appliedAt.format(DateTimeFormatter.ofPattern("yyyy-MM")) : null;
    this.createdAt = createdAt;
  }
}
