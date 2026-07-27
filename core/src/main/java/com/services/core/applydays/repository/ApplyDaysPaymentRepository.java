package com.services.core.applydays.repository;

import com.services.core.applydays.entity.subscription.ApplyDaysPayment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplyDaysPaymentRepository extends JpaRepository<ApplyDaysPayment, UUID> {
  boolean existsByPortonePaymentId(String portonePaymentId);

  Optional<ApplyDaysPayment> findByPortonePaymentId(String portonePaymentId);

  List<ApplyDaysPayment> findAllByMemberIdOrderByCreatedAtDesc(UUID memberId);
}
