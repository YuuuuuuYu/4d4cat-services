package com.services.core.applydays.repository;

import com.services.core.applydays.entity.Application;
import com.services.core.applydays.entity.VerificationStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApplicationRepository
    extends JpaRepository<Application, UUID>, ApplicationRepositoryCustom {
  @Query(
      "SELECT a FROM Application a JOIN VerificationRequest vr ON a.id = vr.applicationId "
          + "WHERE vr.memberId = :memberId AND (:status IS NULL OR a.verificationStatus = :status)")
  Page<Application> findByMemberIdAndStatus(
      @Param("memberId") UUID memberId,
      @Param("status") VerificationStatus status,
      Pageable pageable);

  @Query(
      "SELECT a.verificationStatus, COUNT(a) FROM Application a JOIN VerificationRequest vr ON a.id = vr.applicationId "
          + "WHERE vr.memberId = :memberId GROUP BY a.verificationStatus")
  List<Object[]> countByVerificationStatusForMember(@Param("memberId") UUID memberId);

  @Modifying(clearAutomatically = false, flushAutomatically = true)
  @Query("UPDATE Application a SET a.companySlug = :newSlug WHERE a.companySlug = :oldSlug")
  void updateCompanySlug(@Param("oldSlug") String oldSlug, @Param("newSlug") String newSlug);

  List<Application> findAllByCompanySlugAndVerificationStatus(
      String companySlug, VerificationStatus status);
}
