package com.services.api.applydays.dto;

import com.services.core.applydays.dto.HiringStepDetail;
import com.services.core.applydays.entity.Application;
import com.services.core.applydays.entity.ApplicationChannel;
import com.services.core.applydays.entity.VerificationStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record MyApplicationResponse(
    UUID id,
    String companySlug,
    String companyName,
    Long categoryId,
    String positionDetail,
    LocalDateTime appliedAt,
    List<HiringStepDetail> hiringProcess,
    VerificationStatus verificationStatus,
    String rejectionReason,
    ApplicationChannel channel) {
  public static MyApplicationResponse of(
      Application application, String companyName, String rejectionReason) {
    return MyApplicationResponse.builder()
        .id(application.getId())
        .companySlug(application.getCompanySlug())
        .companyName(companyName != null ? companyName : application.getCompanySlug())
        .categoryId(application.getCategoryId())
        .positionDetail(application.getPositionDetail())
        .appliedAt(application.getAppliedAt())
        .hiringProcess(application.getHiringProcess())
        .verificationStatus(application.getVerificationStatus())
        .rejectionReason(rejectionReason)
        .channel(application.getChannel())
        .build();
  }

  public static MyApplicationResponse of(
      com.services.core.applydays.repository.ApplicationSummary application,
      String companyName,
      String rejectionReason) {
    return MyApplicationResponse.builder()
        .id(application.getId())
        .companySlug(application.getCompanySlug())
        .companyName(companyName != null ? companyName : application.getCompanySlug())
        .categoryId(application.getCategoryId())
        .positionDetail(application.getPositionDetail())
        .appliedAt(application.getAppliedAt())
        .hiringProcess(application.getHiringProcess())
        .verificationStatus(application.getVerificationStatus())
        .rejectionReason(rejectionReason)
        .channel(application.getChannel())
        .build();
  }
}
