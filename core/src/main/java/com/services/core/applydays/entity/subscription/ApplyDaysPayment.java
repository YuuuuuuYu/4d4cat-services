package com.services.core.applydays.entity.subscription;

import com.services.core.common.persistence.BaseSoftDeleteEntity;
import com.services.core.common.persistence.converter.CryptoConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "applydays_payment")
public class ApplyDaysPayment extends BaseSoftDeleteEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "subscription_id", nullable = false)
  private UUID subscriptionId;

  @Column(name = "member_id", nullable = false)
  private UUID memberId;

  @Column(nullable = false)
  private Long amount;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PaymentStatus status;

  @Column(name = "portone_imp_uid", unique = true)
  private String portonePaymentId;

  @Column(name = "portone_merchant_uid")
  private String portoneMerchantUid;

  @Column(name = "fail_reason", length = 2000)
  private String failReason;

  @Column(name = "receipt_url", length = 1000)
  private String receiptUrl;

  @Column(name = "approval_no")
  private String approvalNo;

  private UUID paymentPlanId;

  @Convert(converter = CryptoConverter.class)
  private String pendingBillingKey;

  private boolean renewal;

  private LocalDateTime chargeRequestedAt;

  public void prepare(UUID planId, String billingKey, boolean renewal) {
    this.paymentPlanId = planId;
    this.pendingBillingKey = billingKey;
    this.renewal = renewal;
    markChargeRequested();
  }

  public void markChargeRequested() {
    this.chargeRequestedAt = LocalDateTime.now();
  }

  public void resolve(PaymentStatus status, String reason, String receiptUrl, String approvalNo) {
    this.status = status;
    this.failReason = reason;
    this.receiptUrl = receiptUrl;
    this.approvalNo = approvalNo;
    this.pendingBillingKey = null;
  }

  @Builder
  public ApplyDaysPayment(
      UUID subscriptionId,
      UUID memberId,
      Long amount,
      PaymentStatus status,
      String portonePaymentId,
      String portoneMerchantUid,
      String failReason,
      String receiptUrl,
      String approvalNo) {
    this.subscriptionId = subscriptionId;
    this.memberId = memberId;
    this.amount = amount;
    this.status = status;
    this.portonePaymentId = portonePaymentId;
    this.portoneMerchantUid = portoneMerchantUid;
    this.failReason = failReason;
    this.receiptUrl = receiptUrl;
    this.approvalNo = approvalNo;
  }
}
