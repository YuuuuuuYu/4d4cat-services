package com.services.core.applydays.service;

import com.services.core.applydays.entity.ApplyDaysMemberBenefitType;
import com.services.core.applydays.repository.ApplicationRepository;
import com.services.core.applydays.repository.ApplyDaysMemberBenefitRepository;
import com.services.core.common.persistence.entity.member.Member;
import com.services.core.common.persistence.entity.member.Role;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ApplyDaysSubscriberBenefitCommandService {

  private static final long REQUIRED_DOCUMENT_PASSED_APPLICATION_COUNT = 10;

  private final ApplicationRepository applicationRepository;
  private final ApplyDaysMemberBenefitRepository memberBenefitRepository;

  public void grantIfEligible(Member member) {
    if (member.getRole() != Role.REVIEWER && member.getRole() != Role.SUBSCRIBER) {
      return;
    }

    long approvedDocumentPassedApplicationCount =
        applicationRepository.countApprovedDocumentPassedApplicationsByMemberId(member.getId());
    if (approvedDocumentPassedApplicationCount < REQUIRED_DOCUMENT_PASSED_APPLICATION_COUNT) {
      return;
    }

    int inserted =
        memberBenefitRepository.grantIfAbsent(
            UUID.randomUUID(), member.getId(), ApplyDaysMemberBenefitType.SUBSCRIBER_ACCESS.name());
    if (inserted > 0) {
      log.info("Granted earned subscriber benefit to memberId={}", member.getId());
    }
  }
}
