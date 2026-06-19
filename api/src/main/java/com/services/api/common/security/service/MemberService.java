package com.services.api.common.security.service;

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
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

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
