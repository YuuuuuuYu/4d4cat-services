package com.services.core.applydays.dto;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicSummaryResponse implements Serializable {
  private long totalReviews;
  private long totalCompanies;
  private String message;
}
