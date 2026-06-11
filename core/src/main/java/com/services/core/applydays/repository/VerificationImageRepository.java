package com.services.core.applydays.repository;

import com.services.core.applydays.entity.VerificationImage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VerificationImageRepository extends JpaRepository<VerificationImage, UUID> {
  List<VerificationImage> findAllByApplicationId(UUID applicationId);

  long countByApplicationId(UUID applicationId);

  @Modifying(clearAutomatically = true)
  @Query("UPDATE VerificationImage v SET v.deleted = true WHERE v.applicationId = :applicationId")
  void softDeleteByApplicationId(@Param("applicationId") UUID applicationId);

  @Modifying(clearAutomatically = true)
  @Query("UPDATE VerificationImage v SET v.deleted = true WHERE v.applicationId IN :applicationIds")
  void softDeleteByApplicationIdIn(@Param("applicationIds") List<UUID> applicationIds);

  @Query(value = "SELECT * FROM verification_image WHERE deleted = true", nativeQuery = true)
  List<VerificationImage> findAllDeleted();

  @Modifying(clearAutomatically = true)
  @Query(value = "DELETE FROM verification_image WHERE id IN :ids", nativeQuery = true)
  void hardDeleteByIdIn(@Param("ids") List<UUID> ids);
}
