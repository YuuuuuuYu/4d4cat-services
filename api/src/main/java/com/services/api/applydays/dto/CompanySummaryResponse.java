package com.services.api.applydays.dto;

import com.services.core.applydays.entity.ApplyDaysStatistics;
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
  private ApplyDaysStatistics companyStats;
  private List<ApplyDaysStatistics> categoryL1Stats;
  private List<ApplyDaysStatistics> categoryL2Stats;
}
