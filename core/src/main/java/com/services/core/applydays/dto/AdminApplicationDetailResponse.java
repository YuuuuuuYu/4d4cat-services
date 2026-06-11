package com.services.core.applydays.dto;

import com.services.core.applydays.entity.Application;
import com.services.core.applydays.entity.ApplicationChannel;
import com.services.core.applydays.entity.VerificationStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminApplicationDetailResponse {
  private UUID id;
  private String companySlug;
  private String companyName;
  private Long categoryId;
  private String categoryL1Name;
  private String categoryL2Name;
  private String positionDetail;
  private LocalDateTime appliedAt;
  private List<HiringStepDetail> hiringProcess;
  private VerificationStatus verificationStatus;
  private String rejectionReason;
  private ApplicationChannel channel;

  public static AdminApplicationDetailResponse of(
      Application app, String companyName, String l1Name, String l2Name, String rejectionReason) {
    return AdminApplicationDetailResponse.builder()
        .id(app.getId())
        .companySlug(app.getCompanySlug())
        .companyName(companyName != null ? companyName : app.getCompanySlug())
        .categoryId(app.getCategoryId())
        .categoryL1Name(l1Name)
        .categoryL2Name(l2Name)
        .positionDetail(app.getPositionDetail())
        .appliedAt(app.getAppliedAt())
        .hiringProcess(app.getHiringProcess())
        .verificationStatus(app.getVerificationStatus())
        .rejectionReason(rejectionReason)
        .channel(app.getChannel())
        .build();
  }
}
