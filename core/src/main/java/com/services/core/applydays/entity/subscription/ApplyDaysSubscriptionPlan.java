package com.services.core.applydays.entity.subscription;

import com.services.core.common.persistence.BaseSoftDeleteEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "applydays_subscription_plan")
public class ApplyDaysSubscriptionPlan extends BaseSoftDeleteEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private Long price;

  @Column(name = "billing_cycle_months", nullable = false)
  private Integer billingCycleMonths;

  @Builder
  public ApplyDaysSubscriptionPlan(String name, Long price, Integer billingCycleMonths) {
    this.name = name;
    this.price = price;
    this.billingCycleMonths = billingCycleMonths;
  }
}
