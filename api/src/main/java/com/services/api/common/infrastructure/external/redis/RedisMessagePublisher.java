package com.services.api.common.infrastructure.external.redis;

import com.services.core.common.infrastructure.RedisMessageQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisMessagePublisher {

  private final RedisMessageQueue redisMessageQueue;

  @Value("${applydays.notification.email.trigger-queue}")
  private String triggerQueueName;

  public void publishNotificationBatchTrigger() {
    try {
      redisMessageQueue.push(triggerQueueName, "TRIGGER");
      log.info("Published notification batch trigger to Redis queue: {}", triggerQueueName);
    } catch (Exception e) {
      log.error("Failed to publish notification batch trigger", e);
      throw e;
    }
  }
}
