package com.services.core.applydays.repository;

import com.services.core.applydays.entity.VerificationRequest;
import com.services.core.applydays.entity.VerificationStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface VerificationRequestRepository
    extends JpaRepository<VerificationRequest, UUID>, VerificationRequestRepositoryCustom {
  Optional<VerificationRequest> findByApplicationId(UUID applicationId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT v FROM VerificationRequest v WHERE v.applicationId = :applicationId")
  Optional<VerificationRequest> findByApplicationIdForUpdate(UUID applicationId);

  List<VerificationRequest> findByApplicationIdIn(List<UUID> applicationIds);

  long countByMemberIdAndStatus(UUID memberId, VerificationStatus status);
}
