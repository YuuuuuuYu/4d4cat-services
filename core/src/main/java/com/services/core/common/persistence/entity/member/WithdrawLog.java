package com.services.core.common.persistence.entity.member;

import com.services.core.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "withdraw_log")
public class WithdrawLog extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "member_id", nullable = false)
  private UUID memberId;

  @Column(name = "reason_category", nullable = false)
  private String reasonCategory;

  @Column(name = "reason_detail")
  private String reasonDetail;

  @Builder
  public WithdrawLog(UUID memberId, String reasonCategory, String reasonDetail) {
    this.memberId = memberId;
    this.reasonCategory = reasonCategory;
    this.reasonDetail = reasonDetail;
  }
}
