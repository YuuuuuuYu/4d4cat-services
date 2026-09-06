package com.services.core.applydays.service;

import com.services.core.applydays.entity.ApplyDaysMemberBenefitType;
import com.services.core.applydays.repository.ApplyDaysMemberBenefitRepository;
import com.services.core.common.persistence.entity.member.Member;
import com.services.core.common.persistence.entity.member.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplyDaysSubscriberBenefitQueryService {

  private final ApplyDaysMemberBenefitRepository memberBenefitRepository;

  public boolean hasSubscriberBenefit(Member member) {
    if (member.getRole() == Role.SUBSCRIBER || member.getRole() == Role.ADMIN) {
      return true;
    }
    return memberBenefitRepository.existsByMemberIdAndBenefitType(
        member.getId(), ApplyDaysMemberBenefitType.SUBSCRIBER_ACCESS);
  }
}
