package com.services.core.applydays.entity;

import com.services.core.applydays.dto.HiringStepDetail;
import com.services.core.common.persistence.BaseSoftDeleteEntity;
import com.services.core.common.persistence.converter.CryptoConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted = false")
@SQLDelete(sql = "UPDATE application SET deleted = true WHERE id = ?")
@Table(name = "application")
public class Application extends BaseSoftDeleteEntity implements Persistable<UUID> {

  @Id private UUID id;

  @Column(name = "company_slug", nullable = false)
  private String companySlug;

  @Column(name = "category_id", nullable = false)
  private Long categoryId;

  @Column(name = "applied_at", nullable = false)
  private LocalDateTime appliedAt;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "hiring_process", columnDefinition = "jsonb")
  private List<HiringStepDetail> hiringProcess;

  @Enumerated(EnumType.STRING)
  @Column(name = "verification_status", nullable = false)
  private VerificationStatus verificationStatus = VerificationStatus.PENDING;

  @Column(name = "position_detail", nullable = false)
  private String positionDetail;

  @Enumerated(EnumType.STRING)
  @Column(name = "channel", nullable = false)
  private ApplicationChannel channel;

  @Convert(converter = CryptoConverter.class)
  @Column(name = "access_password")
  private String accessPassword;

  @Builder
  public Application(
      UUID id,
      String companySlug,
      Long categoryId,
      String positionDetail,
      LocalDateTime appliedAt,
      List<HiringStepDetail> hiringProcess,
      ApplicationChannel channel) {
    this.id = id;
    this.companySlug = companySlug;
    this.categoryId = categoryId;
    this.positionDetail = positionDetail;
    this.appliedAt = appliedAt;
    this.hiringProcess = hiringProcess;
    this.channel = channel;
    this.verificationStatus = VerificationStatus.PENDING;
  }

  @Override
  public boolean isNew() {
    return getCreatedAt() == null;
  }

  public void update(
      String companySlug,
      Long categoryId,
      String positionDetail,
      LocalDateTime appliedAt,
      List<HiringStepDetail> hiringProcess,
      ApplicationChannel channel) {
    this.companySlug = companySlug;
    this.categoryId = categoryId;
    this.positionDetail = positionDetail;
    this.appliedAt = appliedAt;
    this.hiringProcess = hiringProcess;
    this.channel = channel;
  }

  public void setAccessPassword(String accessPassword) {
    this.accessPassword = accessPassword;
  }

  public void updateCompanySlug(String newSlug) {
    this.companySlug = newSlug;
  }

  public void approve() {
    this.verificationStatus = VerificationStatus.APPROVED;
  }

  public void reject() {
    this.verificationStatus = VerificationStatus.REJECTED;
  }
}
