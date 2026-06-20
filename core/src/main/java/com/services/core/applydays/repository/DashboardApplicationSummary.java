package com.services.core.applydays.repository;

import com.services.core.applydays.entity.ApplicationChannel;
import com.services.core.applydays.entity.VerificationStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public interface DashboardApplicationSummary {
  UUID getId();

  String getCompanySlug();

  Long getCategoryId();

  LocalDateTime getAppliedAt();

  String getHiringProcess();

  VerificationStatus getVerificationStatus();

  String getPositionDetail();

  ApplicationChannel getChannel();
}
