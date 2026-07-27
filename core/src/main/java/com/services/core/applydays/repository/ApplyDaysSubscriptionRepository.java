package com.services.core.applydays.repository;

import com.services.core.applydays.entity.subscription.ApplyDaysSubscription;
import com.services.core.applydays.entity.subscription.SubscriptionStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApplyDaysSubscriptionRepository
    extends JpaRepository<ApplyDaysSubscription, UUID> {
  Optional<ApplyDaysSubscription> findByMemberId(UUID memberId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT s FROM ApplyDaysSubscription s WHERE s.memberId = :memberId")
  Optional<ApplyDaysSubscription> findWithLockByMemberId(@Param("memberId") UUID memberId);

  Slice<ApplyDaysSubscription> findAllByStatusAndNextBillingDateLessThanEqual(
      SubscriptionStatus status, LocalDate date, Pageable pageable);

  Slice<ApplyDaysSubscription> findAllByStatusAndEndDateLessThan(
      SubscriptionStatus status, LocalDate date, Pageable pageable);
}
