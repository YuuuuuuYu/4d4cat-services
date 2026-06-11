package com.services.api.admin.application.service;

import com.services.api.applydays.service.R2Service;
import com.services.core.applydays.dto.AdminPendingRequestResponse;
import com.services.core.applydays.entity.Application;
import com.services.core.applydays.entity.VerificationRequest;
import com.services.core.applydays.entity.VerificationStatus;
import com.services.core.applydays.repository.ApplicationRepository;
import com.services.core.applydays.repository.VerificationImageRepository;
import com.services.core.applydays.repository.VerificationRequestRepository;
import com.services.core.common.exception.ErrorCode;
import com.services.core.common.exception.NotFoundException;
import com.services.core.common.persistence.entity.member.Role;
import com.services.core.common.persistence.repository.member.MemberRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminApplicationService {

  private final ApplicationRepository applicationRepository;
  private final VerificationRequestRepository verificationRequestRepository;
  private final MemberRepository memberRepository;
  private final VerificationImageRepository verificationImageRepository;
  private final R2Service r2Service;

  public Slice<AdminPendingRequestResponse> getPendingRequests(Pageable pageable) {
    return verificationRequestRepository.findAllWithDetailsByStatus(
        VerificationStatus.PENDING, pageable);
  }

  @Transactional
  public void approveRequest(UUID requestId) {
    log.info("Approving verification request: {}", requestId);
    VerificationRequest request =
        verificationRequestRepository
            .findById(requestId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.APPLICATION_NOT_FOUND));

    request.approve();

    applicationRepository.findById(request.getApplicationId()).ifPresent(Application::approve);

    memberRepository
        .findById(request.getMemberId())
        .ifPresent(
            member -> {
              if (member.getRole() == Role.USER) {
                member.promoteToReviewer();
              }
            });
  }

  @Transactional
  public void rejectRequest(UUID requestId, String reason) {
    log.info("Rejecting verification request: {} for reason: {}", requestId, reason);
    VerificationRequest request =
        verificationRequestRepository
            .findById(requestId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.APPLICATION_NOT_FOUND));

    request.reject(reason);

    applicationRepository.findById(request.getApplicationId()).ifPresent(Application::reject);
  }

  public byte[] getImageBytes(UUID imageId) {
    return verificationImageRepository
        .findById(imageId)
        .map(image -> r2Service.getImageBytes(image.getImageUrl()))
        .orElseThrow(() -> new NotFoundException(ErrorCode.DATA_NOT_FOUND));
  }
}
