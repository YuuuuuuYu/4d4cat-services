package com.services.core.applydays.entity.subscription;

import com.services.core.common.persistence.BaseSoftDeleteEntity;
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
@Table(name = "applydays_payment_method")
public class ApplyDaysPaymentMethod extends BaseSoftDeleteEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "member_id", nullable = false, unique = true)
  private UUID memberId;

  @Column(name = "billing_key", nullable = false)
  private String billingKey;

  @Column(name = "card_company")
  private String cardCompany;

  @Column(name = "card_number_masked")
  private String cardNumberMasked;

  @Column(name = "is_default", nullable = false)
  private boolean isDefault;

  @Builder
  public ApplyDaysPaymentMethod(
      UUID memberId,
      String billingKey,
      String cardCompany,
      String cardNumberMasked,
      boolean isDefault) {
    this.memberId = memberId;
    this.billingKey = billingKey;
    this.cardCompany = cardCompany;
    this.cardNumberMasked = cardNumberMasked;
    this.isDefault = isDefault;
  }

  public void updatePaymentMethod(
      String billingKey, String cardCompany, String cardNumberMasked, boolean isDefault) {
    this.billingKey = billingKey;
    this.cardCompany = cardCompany;
    this.cardNumberMasked = cardNumberMasked;
    this.isDefault = isDefault;
  }
}
