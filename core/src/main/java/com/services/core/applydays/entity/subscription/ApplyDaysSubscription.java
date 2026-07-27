package com.services.core.applydays.entity.subscription;

import com.services.core.common.persistence.BaseSoftDeleteEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "applydays_subscription")
public class ApplyDaysSubscription extends BaseSoftDeleteEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "member_id", nullable = false, unique = true)
  private UUID memberId;

  @Column(name = "plan_id", nullable = false)
  private UUID planId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SubscriptionStatus status;

  @Column(name = "start_date")
  private LocalDate startDate;

  @Column(name = "end_date")
  private LocalDate endDate;

  @Column(name = "next_billing_date")
  private LocalDate nextBillingDate;

  @Builder
  public ApplyDaysSubscription(
      UUID memberId,
      UUID planId,
      SubscriptionStatus status,
      LocalDate startDate,
      LocalDate endDate,
      LocalDate nextBillingDate) {
    this.memberId = memberId;
    this.planId = planId;
    this.status = status;
    this.startDate = startDate;
    this.endDate = endDate;
    this.nextBillingDate = nextBillingDate;
  }

  public void activate(LocalDate startDate, LocalDate endDate, LocalDate nextBillingDate) {
    this.status = SubscriptionStatus.ACTIVE;
    this.startDate = startDate;
    this.endDate = endDate;
    this.nextBillingDate = nextBillingDate;
  }

  public void cancel() {
    this.status = SubscriptionStatus.CANCELED;
    this.nextBillingDate = null;
  }

  public void expire() {
    this.status = SubscriptionStatus.EXPIRED;
    this.nextBillingDate = null;
  }

  public void markPaymentFailed() {
    this.status = SubscriptionStatus.PAYMENT_FAILED;
    this.nextBillingDate = null;
  }

  public void renew(LocalDate startDate, LocalDate endDate, LocalDate nextBillingDate) {
    this.status = SubscriptionStatus.ACTIVE;
    this.startDate = startDate;
    this.endDate = endDate;
    this.nextBillingDate = nextBillingDate;
  }

  public void preRegister(UUID planId) {
    this.planId = planId;
    this.status = SubscriptionStatus.PRE_REGISTERED;
  }

  public void resume() {
    this.status = SubscriptionStatus.ACTIVE;
    this.nextBillingDate = this.endDate.plusDays(1);
  }
}
