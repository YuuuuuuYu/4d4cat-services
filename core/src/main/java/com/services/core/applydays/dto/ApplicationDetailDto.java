package com.services.core.applydays.dto;

import com.services.core.applydays.entity.Application;
import com.services.core.applydays.entity.ApplicationChannel;
import com.services.core.applydays.entity.VerificationStatus;
import java.io.Serializable;
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
public class ApplicationDetailDto implements Serializable {
  private UUID id;
  private String companySlug;
  private Long categoryId;
  private String categoryName;
  private LocalDateTime appliedAt;
  private List<HiringStepDetail> hiringProcess;
  private VerificationStatus verificationStatus;
  private String positionDetail;
  private ApplicationChannel channel;

  public static ApplicationDetailDto from(Application entity, String categoryName) {
    return ApplicationDetailDto.builder()
        .id(entity.getId())
        .companySlug(entity.getCompanySlug())
        .categoryId(entity.getCategoryId())
        .categoryName(categoryName)
        .appliedAt(entity.getAppliedAt())
        .hiringProcess(entity.getHiringProcess())
        .verificationStatus(entity.getVerificationStatus())
        .positionDetail(entity.getPositionDetail())
        .channel(entity.getChannel())
        .build();
  }
}
