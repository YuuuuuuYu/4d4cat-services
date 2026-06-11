package com.services.api.applydays.service;

import com.services.core.applydays.dto.AdminApplicationDetailResponse;
import com.services.core.applydays.dto.AdminApplicationResponse;
import com.services.core.applydays.dto.AdminPendingRequestResponse;
import com.services.core.applydays.entity.VerificationStatus;
import com.services.core.applydays.repository.ApplicationRepository;
import com.services.core.applydays.repository.NotificationQueueRepository;
import com.services.core.applydays.repository.VerificationRequestRepository;
import com.services.core.common.exception.ErrorCode;
import com.services.core.common.exception.NotFoundException;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminApplyDaysQueryService {

  private final ApplicationRepository applicationRepository;
  private final VerificationRequestRepository verificationRequestRepository;
  private final NotificationQueueRepository notificationQueueRepository;

  public int getPendingNotificationCount() {
    return notificationQueueRepository.countByStatus("PENDING");
  }

  public Slice<AdminPendingRequestResponse> getPendingRequests(Pageable pageable) {
    return verificationRequestRepository.findAllWithDetailsByStatus(
        VerificationStatus.PENDING, pageable);
  }

  public Slice<AdminApplicationResponse> getAllApplications(
      String companyName, LocalDateTime appliedAt, Pageable pageable) {
    return applicationRepository.findAllWithDetails(companyName, appliedAt, pageable);
  }

  public AdminApplicationDetailResponse getApplicationDetail(UUID id) {
    return applicationRepository
        .findApplicationDetailById(id)
        .orElseThrow(() -> new NotFoundException(ErrorCode.APPLICATION_NOT_FOUND));
  }
}
