package com.services.core.common.notification.email.template;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

public class SubscriptionEmailTemplate {

  private static final DateTimeFormatter SIMPLE_DATE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd");

  public static String getPaymentSuccessHtmlBase64(
      String userName,
      String planName,
      long amount,
      LocalDateTime paymentDate,
      LocalDate nextBillingDate,
      String receiptUrl) {
    String receiptButton = "";
    if (receiptUrl != null && !receiptUrl.isBlank()) {
      receiptButton =
          "<div style=\"margin-top: 20px; text-align: center;\">"
              + "<a href=\""
              + receiptUrl
              + "\" target=\"_blank\" style=\"display: inline-block; padding: 12px 24px; background-color: #1a73e8; color: #ffffff; text-decoration: none; border-radius: 6px; font-weight: bold; font-size: 14px;\">결제 영수증 / 청구서 확인하기</a>"
              + "</div>";
    }

    String html =
        "<html><body style=\"font-family: Arial, sans-serif; line-height: 1.6; color: #333;\">"
            + "<div style=\"max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 8px;\">"
            + "<h2 style=\"color: #1a73e8; border-bottom: 2px solid #1a73e8; padding-bottom: 10px;\">[ApplyDays] 구독 결제 완료 안내</h2>"
            + "<p>안녕하세요, <strong>"
            + userName
            + "</strong>님!</p>"
            + "<p>ApplyDays Premium 정기 구독 결제가 성공적으로 완료되었습니다.</p>"
            + "<div style=\"background-color: #f9f9f9; padding: 15px; border-radius: 5px; margin: 20px 0;\">"
            + "<ul style=\"list-style: none; padding: 0; margin: 0;\">"
            + "<li style=\"margin-bottom: 8px;\"><b>상품명:</b> "
            + planName
            + "</li>"
            + "<li style=\"margin-bottom: 8px;\"><b>결제 금액:</b> "
            + String.format("%,d", amount)
            + "원</li>"
            + "<li style=\"margin-bottom: 8px;\"><b>결제 일시:</b> "
            + paymentDate.format(SIMPLE_DATE_FORMATTER)
            + "</li>"
            + "<li style=\"margin-bottom: 8px;\"><b>다음 결제 예정일:</b> "
            + nextBillingDate.format(SIMPLE_DATE_FORMATTER)
            + "</li>"
            + "</ul>"
            + "</div>"
            + receiptButton
            + "<p style=\"margin-top: 20px;\">구독해주셔서 감사합니다. 더욱 유용한 서비스로 보답하겠습니다.</p>"
            + "</div>"
            + "</body></html>";
    return Base64.getEncoder().encodeToString(html.getBytes(StandardCharsets.UTF_8));
  }

  public static String getPaymentSuccessHtmlBase64(
      String userName,
      String planName,
      long amount,
      LocalDateTime paymentDate,
      LocalDate nextBillingDate) {
    return getPaymentSuccessHtmlBase64(
        userName, planName, amount, paymentDate, nextBillingDate, null);
  }

  public static String getCancellationHtmlBase64(
      String userName, String planName, LocalDate endDate) {
    String html =
        "<html><body style=\"font-family: Arial, sans-serif; line-height: 1.6; color: #333;\">"
            + "<div style=\"max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 8px;\">"
            + "<h2 style=\"color: #d93025; border-bottom: 2px solid #d93025; padding-bottom: 10px;\">[ApplyDays] 구독 해지 완료 안내</h2>"
            + "<p>안녕하세요, <strong>"
            + userName
            + "</strong>님!</p>"
            + "<p>ApplyDays Premium 정기 구독의 해지 예약 처리가 완료되었습니다.</p>"
            + "<div style=\"background-color: #f9f9f9; padding: 15px; border-radius: 5px; margin: 20px 0;\">"
            + "<ul style=\"list-style: none; padding: 0; margin: 0;\">"
            + "<li style=\"margin-bottom: 8px;\"><b>상품명:</b> "
            + planName
            + "</li>"
            + "<li style=\"margin-bottom: 8px;\"><b>구독 만료일:</b> "
            + endDate.format(SIMPLE_DATE_FORMATTER)
            + "</li>"
            + "</ul>"
            + "</div>"
            + "<p>정기 구독은 해지되었으나, 이미 결제하신 이번 달 주기가 끝날 때까지(만료일까지)는 구독자 혜택을 정상적으로 이용하실 수 있습니다.</p>"
            + "<p>언제든지 다시 구독 신청을 하실 수 있습니다. 이용해 주셔서 감사합니다.</p>"
            + "</div>"
            + "</body></html>";
    return Base64.getEncoder().encodeToString(html.getBytes(StandardCharsets.UTF_8));
  }

  public static String getPaymentFailureHtmlBase64(
      String userName, String planName, String failReason) {
    String html =
        "<html><body style=\"font-family: Arial, sans-serif; line-height: 1.6; color: #333;\">"
            + "<div style=\"max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 8px;\">"
            + "<h2 style=\"color: #ea4335; border-bottom: 2px solid #ea4335; padding-bottom: 10px;\">[ApplyDays] 정기 결제 실패 및 구독 보류 안내</h2>"
            + "<p>안녕하세요, <strong>"
            + userName
            + "</strong>님!</p>"
            + "<p>ApplyDays Premium 정기 구독 자동 결제 처리가 실패하였습니다.</p>"
            + "<div style=\"background-color: #f9f9f9; padding: 15px; border-radius: 5px; margin: 20px 0;\">"
            + "<ul style=\"list-style: none; padding: 0; margin: 0;\">"
            + "<li style=\"margin-bottom: 8px;\"><b>상품명:</b> "
            + planName
            + "</li>"
            + "<li style=\"margin-bottom: 8px;\"><b>실패 사유:</b> <span style=\"color: #d93025;\">"
            + failReason
            + "</span></li>"
            + "</ul>"
            + "</div>"
            + "<p>결제 실패로 인해 구독 혜택 이용이 보류 상태로 전환되었습니다. 정상적인 이용을 위해 결제 카드 상태를 확인해 주시기 바랍니다.</p>"
            + "</div>"
            + "</body></html>";
    return Base64.getEncoder().encodeToString(html.getBytes(StandardCharsets.UTF_8));
  }

  public static String getSubscriptionExpiredHtmlBase64(
      String userName, String planName, LocalDate endDate) {
    String html =
        "<html><body style=\"font-family: Arial, sans-serif; line-height: 1.6; color: #333;\">"
            + "<div style=\"max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 8px;\">"
            + "<h2 style=\"color: #5f6368; border-bottom: 2px solid #5f6368; padding-bottom: 10px;\">[ApplyDays] 구독 만료 안내</h2>"
            + "<p>안녕하세요, <strong>"
            + userName
            + "</strong>님!</p>"
            + "<p>ApplyDays Premium 구독 기간이 만료되었습니다.</p>"
            + "<div style=\"background-color: #f9f9f9; padding: 15px; border-radius: 5px; margin: 20px 0;\">"
            + "<ul style=\"list-style: none; padding: 0; margin: 0;\">"
            + "<li style=\"margin-bottom: 8px;\"><b>상품명:</b> "
            + planName
            + "</li>"
            + "<li style=\"margin-bottom: 8px;\"><b>만료 일시:</b> "
            + endDate.format(SIMPLE_DATE_FORMATTER)
            + "</li>"
            + "</ul>"
            + "</div>"
            + "<p>구독 혜택 및 상세 직군 정보 조회 등의 서비스 권한이 회수되었습니다. 지속적인 혜택 이용을 원하시면 다시 구독 신청을 진행해 주시기 바랍니다.</p>"
            + "<p>감사합니다.</p>"
            + "</div>"
            + "</body></html>";
    return Base64.getEncoder().encodeToString(html.getBytes(StandardCharsets.UTF_8));
  }
}
