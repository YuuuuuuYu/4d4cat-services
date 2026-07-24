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

  private static final long serialVersionUID = 1L;

  private long totalReviews;
  private long totalCompanies;
}
