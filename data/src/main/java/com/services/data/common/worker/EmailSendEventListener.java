package com.services.data.common.worker;

import com.services.core.common.infrastructure.RedisMessageQueue;
import com.services.core.common.notification.email.EmailNotificationService;
import com.services.core.common.notification.email.dto.EmailSendEvent;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailSendEventListener {

  private final RedisMessageQueue redisMessageQueue;
  private final EmailNotificationService emailNotificationService;

  @Value("${app.notification.email.queue-name}")
  private String emailQueueName;

  private Thread pollingThread;
  private volatile boolean running = true;

  @PostConstruct
  public void init() {
    pollingThread = Thread.ofVirtual().unstarted(this::pollMessages);
    pollingThread.setName("email-send-event-listener");
    pollingThread.start();
  }

  @PreDestroy
  public void shutdown() {
    log.info("Shutting down Email send event queue polling...");
    running = false;
    if (pollingThread != null) {
      pollingThread.interrupt();
    }
  }

  private void pollMessages() {
    log.info("Starting Email send event queue polling (Scheduled): {}", emailQueueName);
    while (running && !Thread.currentThread().isInterrupted()) {
      try {
        processScheduledEmails();
        Thread.sleep(5000);
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
        break;
      } catch (Exception e) {
        if (!running) break;
        log.error("Error polling Redis scheduled messages: {}", emailQueueName, e);
        try {
          Thread.sleep(5000);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
        }
      }
    }
    log.info("Stopped Email send event queue polling: {}", emailQueueName);
  }

  void processScheduledEmails() {
    double now = (double) System.currentTimeMillis();
    List<EmailSendEvent> events = redisMessageQueue.zPopByScore(emailQueueName, now);

    if (!events.isEmpty()) {
      log.info("Processing {} scheduled email events", events.size());
      for (EmailSendEvent event : events) {
        try {
          emailNotificationService.sendEmail(
              event.getToEmail(), event.getToNickname(), event.getSubject(), event.getContent());
        } catch (Exception e) {
          log.error("Failed to process email send event for: {}", event.getToEmail(), e);
        }
      }
    }
  }
}
