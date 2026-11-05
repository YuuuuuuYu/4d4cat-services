package com.services.core.applydays.repository;

import com.services.core.applydays.entity.subscription.ApplyDaysSubscription;
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

  @Query(
      """
      SELECT s FROM ApplyDaysSubscription s
      WHERE s.status = com.services.core.applydays.entity.subscription.SubscriptionStatus.ACTIVE
      AND s.nextBillingDate <= :today AND s.memberId > :cursor ORDER BY s.memberId
      """)
  Slice<ApplyDaysSubscription> findRenewals(LocalDate today, UUID cursor, Pageable pageable);

  @Query(
      """
      SELECT s FROM ApplyDaysSubscription s
      WHERE s.status = com.services.core.applydays.entity.subscription.SubscriptionStatus.CANCELED
      AND s.endDate < :today AND s.memberId > :cursor ORDER BY s.memberId
      """)
  Slice<ApplyDaysSubscription> findExpirations(LocalDate today, UUID cursor, Pageable pageable);
}
