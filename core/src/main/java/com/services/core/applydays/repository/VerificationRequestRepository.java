package com.services.core.applydays.repository;

import com.services.core.applydays.entity.VerificationRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationRequestRepository
    extends JpaRepository<VerificationRequest, UUID>, VerificationRequestRepositoryCustom {
  Optional<VerificationRequest> findByApplicationId(UUID applicationId);

  List<VerificationRequest> findByApplicationIdIn(List<UUID> applicationIds);
}
