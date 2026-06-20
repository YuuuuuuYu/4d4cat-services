package com.services.api.applydays.dto;

import com.services.core.applydays.dto.MyApplicationsSummaryResponse;
import com.services.core.common.dto.PageResponse;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MyApplicationsDashboardResponse {
  private final MyApplicationsSummaryResponse summary;
  private final PageResponse<MyApplicationResponse> pendingApplications;
  private final PageResponse<MyApplicationResponse> approvedApplications;
  private final PageResponse<MyApplicationResponse> rejectedApplications;
}
