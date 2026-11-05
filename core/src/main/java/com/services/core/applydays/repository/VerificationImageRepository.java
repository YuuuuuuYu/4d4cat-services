package com.services.core.applydays.repository;

import com.services.core.applydays.entity.VerificationImage;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface VerificationImageRepository extends JpaRepository<VerificationImage, UUID> {
  List<VerificationImage> findAllByApplicationId(UUID applicationId);

  @Query(
      """
      SELECT count(i) FROM VerificationImage i WHERE i.applicationId = :applicationId
      AND (i.reservedUntil IS NULL OR i.reservedUntil > :now)
      """)
  long countActiveSlots(UUID applicationId, LocalDateTime now);

  @Query(
      value =
          "SELECT vi.id as id, vi.image_url as imageUrl FROM verification_image vi "
              + "INNER JOIN application a ON vi.application_id = a.id "
              + "WHERE (vi.reserved_until IS NOT NULL AND vi.reserved_until <= :now) "
              + "OR (vi.reserved_until IS NULL AND (a.deleted = true OR EXISTS ("
              + "SELECT 1 FROM verification_request vr JOIN member m ON m.id = vr.member_id "
              + "WHERE vr.application_id = a.id AND m.deleted = true)))",
      nativeQuery = true)
  List<DeletedImageProjection> findImagesPendingPurge(LocalDateTime now);
}
