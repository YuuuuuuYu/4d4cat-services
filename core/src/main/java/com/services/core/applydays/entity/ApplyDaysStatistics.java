package com.services.core.applydays.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "apply_days_statistics")
public class ApplyDaysStatistics implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "company_slug", nullable = false)
  private String companySlug;

  @Column(name = "category_id")
  private Long categoryId;

  @Column(name = "review_count", nullable = false)
  private Integer reviewCount;

  @Column(name = "ghosting_count", nullable = false)
  private Integer ghostingCount;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "step_statistics", nullable = false, columnDefinition = "jsonb")
  private String stepStatistics;

  @Column(name = "stat_type", nullable = false)
  private String statType;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;
}
