package com.services.api.applydays.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.services.core.applydays.dto.ApplyDaysStatisticsResponse;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CompanySummaryResponse implements Serializable {
  private String slug;
  private String name;
  private ApplyDaysStatisticsResponse companyStats;
  private List<ApplyDaysStatisticsResponse> categoryL1Stats;
  private List<ApplyDaysStatisticsResponse> categoryL2Stats;
}
