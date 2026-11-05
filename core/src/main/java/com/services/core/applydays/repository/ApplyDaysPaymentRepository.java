package com.services.core.applydays.repository;

import com.services.core.applydays.entity.subscription.ApplyDaysPayment;
import com.services.core.applydays.entity.subscription.PaymentStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplyDaysPaymentRepository extends JpaRepository<ApplyDaysPayment, UUID> {
  Optional<ApplyDaysPayment> findByPortonePaymentId(String portonePaymentId);

  Optional<ApplyDaysPayment> findFirstByMemberIdAndStatusOrderByCreatedAtAsc(
      UUID memberId, PaymentStatus status);

  boolean existsByMemberIdAndStatus(UUID memberId, PaymentStatus status);

  Slice<ApplyDaysPayment> findByStatusAndIdGreaterThanOrderById(
      PaymentStatus status, UUID id, Pageable pageable);

  List<ApplyDaysPayment> findAllByMemberIdOrderByCreatedAtDesc(UUID memberId);
}
