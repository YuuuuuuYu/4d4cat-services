package com.services.core.applydays.repository;

import com.services.core.applydays.dto.HiringStepDetail;
import com.services.core.applydays.entity.ApplicationChannel;
import com.services.core.applydays.entity.VerificationStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ApplicationSummary {
  UUID getId();

  String getCompanySlug();

  Long getCategoryId();

  LocalDateTime getAppliedAt();

  List<HiringStepDetail> getHiringProcess();

  VerificationStatus getVerificationStatus();

  String getPositionDetail();

  ApplicationChannel getChannel();
}
