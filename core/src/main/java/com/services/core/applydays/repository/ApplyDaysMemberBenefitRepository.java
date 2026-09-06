package com.services.core.applydays.repository;

import com.services.core.applydays.entity.ApplyDaysMemberBenefit;
import com.services.core.applydays.entity.ApplyDaysMemberBenefitType;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApplyDaysMemberBenefitRepository
    extends JpaRepository<ApplyDaysMemberBenefit, UUID> {

  boolean existsByMemberIdAndBenefitType(UUID memberId, ApplyDaysMemberBenefitType benefitType);

  @Modifying
  @Query(
      value =
          "INSERT INTO applydays_member_benefit "
              + "(id, member_id, benefit_type, created_at, updated_at) "
              + "VALUES (:id, :memberId, :benefitType, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) "
              + "ON CONFLICT (member_id, benefit_type) DO NOTHING",
      nativeQuery = true)
  int grantIfAbsent(
      @Param("id") UUID id,
      @Param("memberId") UUID memberId,
      @Param("benefitType") String benefitType);
}
