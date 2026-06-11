package com.services.core.applydays.service;

import com.services.core.applydays.entity.VerificationRequest;
import com.services.core.applydays.entity.VerificationStatus;
import com.services.core.applydays.repository.ApplicationRepository;
import com.services.core.applydays.repository.VerificationRequestRepository;
import com.services.core.common.exception.ErrorCode;
import com.services.core.common.exception.NotFoundException;
import com.services.core.common.persistence.entity.member.Role;
import com.services.core.common.persistence.repository.member.MemberRepository;
import com.services.core.common.util.RandomUtils;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplyDaysWorkerService {

  private final ApplicationRepository applicationRepository;
  private final VerificationRequestRepository verificationRequestRepository;
  private final MemberRepository memberRepository;

  @Transactional
  public void processApproval(UUID applicationId) {
    VerificationRequest request =
        verificationRequestRepository
            .findByApplicationId(applicationId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.APPLICATION_NOT_FOUND));

    if (request.getStatus() == VerificationStatus.APPROVED) {
      log.info("Request for application {} is already APPROVED. Skipping.", applicationId);
      return;
    }

    request.approve();

    applicationRepository
        .findById(applicationId)
        .ifPresent(
            app -> {
              app.approve();
              if (app.getAccessPassword() == null) {
                String rawPassword = RandomUtils.generateRandomAlphanumeric(10);
                app.setAccessPassword(rawPassword);
                log.info("Generated access password for application: {}", app.getId());
              }
            });

    memberRepository
        .findById(request.getMemberId())
        .ifPresent(
            member -> {
              if (member.getRole() == Role.USER) {
                member.promoteToReviewer();
              }
            });

    log.info("Successfully processed approval for application: {}", applicationId);
  }
}
