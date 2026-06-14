package com.services.core.applydays.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.services.core.applydays.entity.ApplyDaysStatistics;
import java.io.Serializable;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApplyDaysStatisticsResponse implements Serializable {
  private Long id;
  private String companySlug;
  private Long categoryId;
  private String categoryName;
  private Integer reviewCount;
  private Integer ghostingCount;
  private String stepStatistics;
  private String statType;
  private OffsetDateTime updatedAt;

  public static ApplyDaysStatisticsResponse from(
      ApplyDaysStatistics entity, String categoryName, boolean includeDetails) {
    return ApplyDaysStatisticsResponse.builder()
        .id(entity.getId())
        .companySlug(entity.getCompanySlug())
        .categoryId(entity.getCategoryId())
        .categoryName(categoryName)
        .reviewCount(entity.getReviewCount())
        .ghostingCount(entity.getGhostingCount())
        .stepStatistics(includeDetails ? entity.getStepStatistics() : null)
        .statType(entity.getStatType())
        .updatedAt(entity.getUpdatedAt())
        .build();
  }
}
