package com.services.core.applydays.dto;

import com.services.core.applydays.entity.ApplicationChannel;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;

@Getter
public class TimelineDetailResponse extends TimelineBasicResponse {
  private final String positionDetail;
  private final ApplicationChannel channel;

  public TimelineDetailResponse(
      UUID id,
      String categoryL1Name,
      String categoryL2Name,
      LocalDateTime appliedAt,
      LocalDateTime createdAt,
      String positionDetail,
      ApplicationChannel channel) {
    super(id, categoryL1Name, categoryL2Name, appliedAt, createdAt);
    this.positionDetail = positionDetail;
    this.channel = channel;
  }
}
