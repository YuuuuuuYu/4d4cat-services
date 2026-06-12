package com.services.api.applydays.dto;

import com.services.core.applydays.dto.ApplyDaysStatisticsDto;
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
public class CompanySummaryResponse implements Serializable {
  private String slug;
  private String name;
  private ApplyDaysStatisticsDto companyStats;
  private List<ApplyDaysStatisticsDto> categoryL1Stats;
  private List<ApplyDaysStatisticsDto> categoryL2Stats;
}
