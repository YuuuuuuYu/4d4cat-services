package com.services.core.applydays.dto;

import com.services.core.applydays.entity.VerificationStatus;
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
public class AdminPendingRequestResponse {
  private UUID requestId;
  private UUID applicationId;
  private LocalDateTime appliedAt;
  private String companyName;
  private String positionDetail;
  private VerificationStatus status;
  private Long categoryId;
  private String categoryL1Name;
  private String categoryL2Name;
}
