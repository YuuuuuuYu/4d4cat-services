package com.services.core.applydays.dto;

import com.services.core.applydays.entity.Application;
import com.services.core.applydays.entity.ApplicationChannel;
import com.services.core.applydays.entity.VerificationStatus;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ApplicationDetailResponse(
    UUID id,
    String companySlug,
    Long categoryId,
    String categoryName,
    LocalDateTime appliedAt,
    List<HiringStepDetail> hiringProcess,
    VerificationStatus verificationStatus,
    String positionDetail,
    ApplicationChannel channel
) implements Serializable {

  public static ApplicationDetailResponse from(Application entity, String categoryName) {
    return new ApplicationDetailResponse(
        entity.getId(),
        entity.getCompanySlug(),
        entity.getCategoryId(),
        categoryName,
        entity.getAppliedAt(),
        entity.getHiringProcess(),
        entity.getVerificationStatus(),
        entity.getPositionDetail(),
        entity.getChannel()
    );
  }
}
