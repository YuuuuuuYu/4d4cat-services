package com.services.api.common.security.service;

import com.services.core.applydays.entity.subscription.PaymentStatus;
import com.services.core.applydays.repository.ApplyDaysPaymentMethodRepository;
import com.services.core.applydays.repository.ApplyDaysPaymentRepository;
import com.services.core.applydays.repository.ApplyDaysSubscriptionRepository;
import com.services.core.common.exception.BadRequestException;
import com.services.core.common.exception.ErrorCode;
import com.services.core.common.exception.NotFoundException;
import com.services.core.common.infrastructure.RedisDataStorage;
import com.services.core.common.persistence.entity.member.Member;
import com.services.core.common.persistence.entity.member.WithdrawLog;
import com.services.core.common.persistence.repository.member.MemberRepository;
import com.services.core.common.persistence.repository.member.WithdrawLogRepository;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

  private final MemberRepository memberRepository;
  private final WithdrawLogRepository withdrawLogRepository;
  private final RedisDataStorage redisDataStorage;
  private final ApplyDaysSubscriptionRepository subscriptionRepository;
  private final ApplyDaysPaymentRepository paymentRepository;
  private final ApplyDaysPaymentMethodRepository paymentMethodRepository;

  @Cacheable(value = "memberId", key = "#email")
  @Transactional(readOnly = true)
  public String getMemberIdByEmail(String email) {
    return memberRepository
        .findByEmail(email)
        .map(Member::getId)
        .map(UUID::toString)
        .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
  }

  @Transactional
  @CacheEvict(value = "memberId", key = "#email")
  public void withdraw(String email, String reasonCategory, String reasonDetail) {
    Member member =
        memberRepository
            .findByEmail(email)
            .flatMap(existing -> memberRepository.findByIdForUpdate(existing.getId()))
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

    if (paymentRepository.existsByMemberIdAndStatus(member.getId(), PaymentStatus.PENDING)) {
      throw new BadRequestException(ErrorCode.PAYMENT_PENDING);
    }
    subscriptionRepository
        .findWithLockByMemberId(member.getId())
        .ifPresent(subscription -> subscription.expire());
    paymentMethodRepository
        .findByMemberIdAndDeletedFalse(member.getId())
        .ifPresent(method -> method.delete());

    WithdrawLog log =
        WithdrawLog.builder()
            .memberId(member.getId())
            .reasonCategory(reasonCategory)
            .reasonDetail(reasonDetail)
            .build();
    withdrawLogRepository.save(log);

    member.anonymize();

    memberRepository.delete(member);

    redisDataStorage.deleteCache("REFRESH_TOKEN:" + email);

    long now = System.currentTimeMillis();
    redisDataStorage.setCache("USER_REVOKED_AT:" + email, String.valueOf(now), 2, TimeUnit.DAYS);
  }
}
