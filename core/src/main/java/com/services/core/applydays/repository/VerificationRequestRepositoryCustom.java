package com.services.core.applydays.repository;

import com.services.core.applydays.dto.AdminPendingRequestResponse;
import com.services.core.applydays.entity.VerificationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface VerificationRequestRepositoryCustom {
  Slice<AdminPendingRequestResponse> findAllWithDetailsByStatus(
      VerificationStatus status, Pageable pageable);
}
