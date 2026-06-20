package com.services.core.applydays.repository;

import com.services.core.applydays.entity.VerificationImage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface VerificationImageRepository extends JpaRepository<VerificationImage, UUID> {
  List<VerificationImage> findAllByApplicationId(UUID applicationId);

  long countByApplicationId(UUID applicationId);

  @Query(
      value =
          "SELECT vi.id as id, vi.image_url as imageUrl FROM verification_image vi "
              + "INNER JOIN application a ON vi.application_id = a.id "
              + "WHERE a.deleted = true",
      nativeQuery = true)
  List<DeletedImageProjection> findAllByApplicationDeletedTrue();
}
