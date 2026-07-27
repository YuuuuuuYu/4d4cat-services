package com.services.data.applydays.worker;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.services.core.applydays.service.ApplyDaysSubscriptionCommandService;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ApplyDaysSubscriptionSchedulerTest {

  @Mock private ApplyDaysSubscriptionCommandService subscriptionService;
  @Mock private StringRedisTemplate redisTemplate;
  @Mock private ValueOperations<String, String> valueOperations;

  private ApplyDaysSubscriptionScheduler scheduler;

  @BeforeEach
  void setUp() {
    scheduler = new ApplyDaysSubscriptionScheduler(subscriptionService, redisTemplate);
    ReflectionTestUtils.setField(scheduler, "lockKey", "lock:subscription-billing");
    ReflectionTestUtils.setField(scheduler, "lockDuration", Duration.ofMinutes(10));
    given(redisTemplate.opsForValue()).willReturn(valueOperations);
  }

  @Test
  @DisplayName("분산락 획득 성공 시 갱신 및 만료 스케줄러 실행")
  void processSubscriptionBillingAndExpiration_lockAcquired_success() {
    // given
    given(
            valueOperations.setIfAbsent(
                eq("lock:subscription-billing"), eq("locked"), any(Duration.class)))
        .willReturn(true);

    // when
    scheduler.processSubscriptionBillingAndExpiration();

    // then
    verify(subscriptionService).processRenewal();
    verify(subscriptionService).processExpiration();
  }

  @Test
  @DisplayName("분산락 획득 실패 시 스케줄러 실행하지 않고 조기 종료")
  void processSubscriptionBillingAndExpiration_lockNotAcquired_skip() {
    // given
    given(
            valueOperations.setIfAbsent(
                eq("lock:subscription-billing"), eq("locked"), any(Duration.class)))
        .willReturn(false);

    // when
    scheduler.processSubscriptionBillingAndExpiration();

    // then
    verify(subscriptionService, never()).processRenewal();
    verify(subscriptionService, never()).processExpiration();
  }
}
