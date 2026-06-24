package com.services.core.applydays.repository;

import com.services.core.applydays.entity.Application;
import com.services.core.applydays.entity.VerificationStatus;
import java.time.LocalDateTime;
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

  long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

  @Query(
      "SELECT a.channel, COUNT(a) FROM Application a WHERE a.createdAt BETWEEN :start AND :end GROUP BY a.channel")
  List<Object[]> countByChannelAndCreatedAtBetween(
      @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

  @Query(
      "SELECT a.id as id, a.companySlug as companySlug, a.categoryId as categoryId, "
          + "a.appliedAt as appliedAt, a.hiringProcess as hiringProcess, "
          + "a.verificationStatus as verificationStatus, a.positionDetail as positionDetail, "
          + "a.channel as channel "
          + "FROM Application a JOIN VerificationRequest vr ON a.id = vr.applicationId "
          + "WHERE vr.memberId = :memberId AND (:status IS NULL OR a.verificationStatus = :status)")
  Page<ApplicationSummary> findByMemberIdAndStatus(
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

  List<ApplicationSummary> findAllByCompanySlugAndVerificationStatus(
      String companySlug, VerificationStatus status);

  @Query(
      value =
          "SELECT id, company_slug as companySlug, category_id as categoryId, "
              + "applied_at as appliedAt, CAST(hiring_process AS text) as hiringProcess, "
              + "verification_status as verificationStatus, position_detail as positionDetail, "
              + "channel "
              + "FROM ( "
              + "  SELECT a.id, a.company_slug, a.category_id, a.applied_at, a.hiring_process, "
              + "         a.verification_status, a.position_detail, a.channel, "
              + "         ROW_NUMBER() OVER (PARTITION BY a.verification_status ORDER BY a.applied_at DESC) as rn "
              + "  FROM application a JOIN verification_request vr ON a.id = vr.application_id "
              + "  WHERE vr.member_id = :memberId AND a.deleted = false "
              + ") tmp WHERE rn <= 10",
      nativeQuery = true)
  List<DashboardApplicationSummary> findDashboardApplications(@Param("memberId") UUID memberId);
}
