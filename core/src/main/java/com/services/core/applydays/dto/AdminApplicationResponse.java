package com.services.core.applydays.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminApplicationResponse {
  private UUID id;
  private String companyName;
  private LocalDateTime appliedAt;
  private String positionDetail;
  private Long categoryId;
  private String categoryL1Name;
  private String categoryL2Name;
}
