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
public class CompanyListResponse implements Serializable {
  private String slug;
  private String name;
  private Integer reviewCount;
  private Integer ghostingCount;
  private String avgResponseTime;
  private Double ghostingRate;
}
