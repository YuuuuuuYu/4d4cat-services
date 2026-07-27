package com.services.api.applydays.dto;

import com.services.core.applydays.entity.subscription.ApplyDaysPayment;
import com.services.core.applydays.entity.subscription.PaymentStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentHistoryResponse(
    UUID id,
    Long amount,
    PaymentStatus status,
    String portonePaymentId,
    String failReason,
    String receiptUrl,
    String approvalNo,
    LocalDateTime createdAt) {
  public static PaymentHistoryResponse from(ApplyDaysPayment payment) {
    return new PaymentHistoryResponse(
        payment.getId(),
        payment.getAmount(),
        payment.getStatus(),
        payment.getPortonePaymentId(),
        payment.getFailReason(),
        payment.getReceiptUrl(),
        payment.getApprovalNo(),
        payment.getCreatedAt());
  }
}
