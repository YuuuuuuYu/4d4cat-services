package com.services.core.applydays.entity;

import com.services.core.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "verification_request")
public class VerificationRequest extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "application_id", nullable = false)
  private UUID applicationId;

  @Column(name = "member_id", nullable = false)
  private UUID memberId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private VerificationStatus status = VerificationStatus.PENDING;

  @Column(name = "rejection_reason")
  private String rejectionReason;

  @Builder
  public VerificationRequest(UUID applicationId, UUID memberId) {
    this.applicationId = applicationId;
    this.memberId = memberId;
    this.status = VerificationStatus.PENDING;
  }

  public void approve() {
    this.status = VerificationStatus.APPROVED;
  }

  public void reject(String reason) {
    this.status = VerificationStatus.REJECTED;
    this.rejectionReason = reason;
  }
}
