package com.services.core.applydays.service;

import com.services.core.applydays.entity.subscription.ApplyDaysPaymentMethod;
import com.services.core.applydays.repository.ApplyDaysPaymentMethodRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplyDaysPaymentMethodQueryService {

  private final ApplyDaysPaymentMethodRepository paymentMethodRepository;

  public Optional<ApplyDaysPaymentMethod> findByMemberId(UUID memberId) {
    return paymentMethodRepository.findByMemberIdAndDeletedFalse(memberId);
  }

  public Optional<ApplyDaysPaymentMethod> findAnyByMemberId(UUID memberId) {
    return paymentMethodRepository.findByMemberId(memberId);
  }

  public boolean hasPaymentMethod(UUID memberId) {
    return paymentMethodRepository.existsByMemberIdAndDeletedFalse(memberId);
  }
}
