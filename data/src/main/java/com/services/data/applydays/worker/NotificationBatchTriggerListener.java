package com.services.data.applydays.worker;

import com.services.core.common.infrastructure.RedisMessageQueue;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationBatchTriggerListener {

  private final RedisMessageQueue redisMessageQueue;
  private final AggregatedNotificationScheduler aggregatedNotificationScheduler;

  @Value("${applydays.notification.email.trigger-queue}")
  private String triggerQueueName;

  private Thread listenerThread;
  private volatile boolean running = true;

  @PostConstruct
  public void init() {
    listenerThread = Thread.ofVirtual().unstarted(this::listenForTriggers);
    listenerThread.setName("notification-batch-trigger-listener");
    listenerThread.start();
  }

  @PreDestroy
  public void shutdown() {
    log.info("Shutting down Notification Batch Trigger listener...");
    running = false;
    if (listenerThread != null) {
      listenerThread.interrupt();
    }
  }

  private void listenForTriggers() {
    log.info("Starting Notification Batch Trigger listener (LPUSH/BRPOP): {}", triggerQueueName);
    while (running && !Thread.currentThread().isInterrupted()) {
      try {
        // Wait for trigger signal (Blocking pop for 5 seconds to stay within commandTimeout)
        redisMessageQueue
            .pop(triggerQueueName, Duration.ofSeconds(5), String.class)
            .ifPresent(
                msg -> {
                  log.info("Received manual batch trigger signal. Executing batch send...");
                  try {
                    aggregatedNotificationScheduler.sendAggregatedEmails();
                  } catch (Exception e) {
                    log.error("Failed to execute manual batch send", e);
                  }
                });
      } catch (Exception e) {
        if (!running) break;
        log.error("Error in Notification Batch Trigger listener", e);
        try {
          Thread.sleep(5000);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
        }
      }
    }
    log.info("Stopped Notification Batch Trigger listener: {}", triggerQueueName);
  }
}
