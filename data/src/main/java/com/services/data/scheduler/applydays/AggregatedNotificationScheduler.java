package com.services.data.scheduler.applydays;

import com.services.core.applydays.entity.Application;
import com.services.core.applydays.entity.NotificationQueue;
import com.services.core.applydays.repository.ApplicationRepository;
import com.services.core.applydays.repository.NotificationQueueRepository;
import com.services.core.common.persistence.entity.member.Member;
import com.services.core.common.persistence.repository.member.MemberRepository;
import com.services.data.applydays.worker.SendPulseNotificationWorker;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AggregatedNotificationScheduler {

  private final NotificationQueueRepository notificationQueueRepository;
  private final ApplicationRepository applicationRepository;
  private final MemberRepository memberRepository;
  private final SendPulseNotificationWorker sendPulseNotificationWorker;

  @Scheduled(cron = "${app.notification.email.batch-cron}")
  @Transactional
  public void sendAggregatedEmails() {
    log.info("Starting aggregated email notification batch...");

    LocalDateTime now = LocalDateTime.now();
    List<NotificationQueue> pendingQueues =
        notificationQueueRepository.findAllByStatus("PENDING").stream()
            .filter(q -> q.getScheduledAt() == null || q.getScheduledAt().isBefore(now))
            .toList();

    if (pendingQueues.isEmpty()) {
      log.info("No pending notifications to send.");
      return;
    }

    log.info("Found {} pending notifications.", pendingQueues.size());

    Map<UUID, List<NotificationQueue>> groups =
        pendingQueues.stream().collect(Collectors.groupingBy(NotificationQueue::getMemberId));

    for (Map.Entry<UUID, List<NotificationQueue>> entry : groups.entrySet()) {
      UUID memberId = entry.getKey();
      List<NotificationQueue> userQueues = entry.getValue();
      List<UUID> applicationIds =
          userQueues.stream().map(NotificationQueue::getApplicationId).toList();

      processUserBatch(memberId, applicationIds, userQueues);
    }

    log.info("Aggregated email notification batch completed.");
  }

  private void processUserBatch(
      UUID memberId, List<UUID> applicationIds, List<NotificationQueue> queues) {
    Member member = memberRepository.findById(memberId).orElse(null);
    if (member == null || member.getEmail() == null) {
      log.warn("Member {} not found or has no email. Skipping notification.", memberId);
      queues.forEach(q -> q.markAsFailed("Member not found or email missing"));
      return;
    }

    try {
      List<Application> applications = applicationRepository.findAllById(applicationIds);

      if (applications.isEmpty()) {
        log.warn(
            "No applications found for the given IDs for member {}. Marking as FAILED.", memberId);
        queues.forEach(q -> q.markAsFailed("Applications not found"));
        return;
      }

      sendPulseNotificationWorker.sendVerificationResultEmail(member, applications);

      queues.forEach(NotificationQueue::markAsSent);
      log.info(
          "Successfully sent aggregated email to member {} for {} applications.",
          memberId,
          applications.size());

    } catch (Exception e) {
      log.error("Failed to send aggregated email for member: {}", memberId, e);
      queues.forEach(q -> q.markAsFailed(e.getMessage()));
    }
  }
}
