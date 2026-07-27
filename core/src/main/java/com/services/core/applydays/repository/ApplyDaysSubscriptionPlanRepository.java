package com.services.core.applydays.repository;

import com.services.core.applydays.entity.subscription.ApplyDaysSubscriptionPlan;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplyDaysSubscriptionPlanRepository
    extends JpaRepository<ApplyDaysSubscriptionPlan, UUID> {
  List<ApplyDaysSubscriptionPlan> findAllByDeletedFalse();
}
