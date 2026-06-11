package com.services.api.applydays.dto;

import com.services.core.applydays.dto.HiringStepDetail;
import com.services.core.applydays.entity.ApplicationChannel;
import java.time.OffsetDateTime;
import java.util.List;

public record ApplicationUpdateRequest(
    String companySlug,
    String companyName,
    Long categoryId,
    String positionDetail,
    OffsetDateTime appliedAt,
    List<HiringStepDetail> hiringProcess,
    ApplicationChannel channel) {}
