package com.services.core.applydays.entity;

import com.services.core.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "notification_queue")
public class NotificationQueue extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "member_id", nullable = false)
  private UUID memberId;

  @Column(name = "application_id", nullable = false)
  private UUID applicationId;

  @Column(name = "notification_type", nullable = false)
  private String notificationType;

  @Column(name = "status", nullable = false)
  private String status = "PENDING";

  @Column(name = "error_message")
  private String errorMessage;

  @Column(name = "scheduled_at")
  private LocalDateTime scheduledAt;

  @Column(name = "sent_at")
  private LocalDateTime sentAt;

  @Builder
  public NotificationQueue(
      UUID memberId, UUID applicationId, String notificationType, LocalDateTime scheduledAt) {
    this.memberId = memberId;
    this.applicationId = applicationId;
    this.notificationType = notificationType;
    this.scheduledAt = scheduledAt;
    this.status = "PENDING";
  }

  public void markAsSent() {
    this.status = "SENT";
    this.sentAt = LocalDateTime.now();
  }

  public void markAsFailed(String errorMessage) {
    this.status = "FAILED";
    this.errorMessage = errorMessage;
  }
}
