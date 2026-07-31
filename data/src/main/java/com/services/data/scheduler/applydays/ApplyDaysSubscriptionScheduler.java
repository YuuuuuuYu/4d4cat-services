package com.services.data.scheduler.applydays;

import com.services.core.applydays.service.ApplyDaysSubscriptionCommandService;
import com.services.core.common.notification.discord.DiscordChannel;
import com.services.core.common.notification.discord.NotifyDiscord;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "app.scheduler.enabled.subscription-billing",
    havingValue = "true",
    matchIfMissing = true)
public class ApplyDaysSubscriptionScheduler {

  private final ApplyDaysSubscriptionCommandService subscriptionService;
  private final StringRedisTemplate redisTemplate;

  @Value("${app.scheduler.lock.subscription-billing.key}")
  private String lockKey;

  @Value("${app.scheduler.lock.subscription-billing.duration}")
  private Duration lockDuration;

  @Scheduled(cron = "10 0 0 * * *")
  @NotifyDiscord(
      taskName = "Subscription Billing & Expiration Processing",
      channel = DiscordChannel.STATISTICS)
  public SubscriptionJobReport processSubscriptionBillingAndExpiration() {
    Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "locked", lockDuration);
    if (acquired == null || !acquired) {
      log.info(
          "Another instance is already running Subscription Billing & Expiration task. Skipping.");
      return new SubscriptionJobReport(0, 0);
    }

    log.info("Starting subscription renewal and expiration scheduler...");
    try {
      // 1. Process renewals (charge ACTIVE subscriptions where nextBillingDate <= now)
      int renewals = subscriptionService.processRenewal();

      // 2. Process expirations (deactivate CANCELED subscriptions where endDate < now)
      int expirations = subscriptionService.processExpiration();

      log.info("Finished subscription renewal and expiration scheduler successfully.");
      return new SubscriptionJobReport(renewals, expirations);
    } catch (Exception e) {
      log.error("Error occurred during subscription billing/expiration task", e);
      throw e;
    } finally {
      redisTemplate.delete(lockKey);
    }
  }
}
