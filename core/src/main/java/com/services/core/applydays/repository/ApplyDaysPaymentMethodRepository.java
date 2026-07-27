package com.services.core.applydays.repository;

import com.services.core.applydays.entity.subscription.ApplyDaysPaymentMethod;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApplyDaysPaymentMethodRepository
    extends JpaRepository<ApplyDaysPaymentMethod, UUID> {

  Optional<ApplyDaysPaymentMethod> findByMemberIdAndDeletedFalse(UUID memberId);

  Optional<ApplyDaysPaymentMethod> findByMemberId(UUID memberId);

  boolean existsByMemberIdAndDeletedFalse(UUID memberId);
}
