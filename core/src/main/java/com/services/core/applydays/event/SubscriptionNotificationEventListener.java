package com.services.core.applydays.event;

import com.services.core.common.infrastructure.external.sendpulse.SendPulseEmailClient;
import com.services.core.common.infrastructure.external.sendpulse.dto.SendPulseEmailRequest;
import com.services.core.common.notification.discord.DiscordChannel;
import com.services.core.common.notification.discord.DiscordWebhookPayload;
import com.services.core.common.notification.discord.DiscordWebhookService;
import com.services.core.common.notification.discord.Embed;
import com.services.core.common.notification.email.template.SubscriptionEmailTemplate;
import com.services.core.common.persistence.repository.member.MemberRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.HtmlUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionNotificationEventListener {

  private final ObjectProvider<SendPulseEmailClient> sendPulseEmailClientProvider;
  private final DiscordWebhookService discordWebhookService;
  private final MemberRepository memberRepository;

  @Value("${app.notification.sendpulse.sender-email}")
  private String senderEmail;

  @Value("${app.notification.sendpulse.sender-name}")
  private String senderName;

  @Async("subscriptionEventTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleSubscriptionPaid(SubscriptionPaidEvent event) {
    log.info("Handling SubscriptionPaidEvent for memberId={}", event.memberId());

    String name = sanitizeText(event.memberName());
    String email = sanitizeText(event.memberEmail());
    String planName = sanitizeText(event.planName());

    String subject = event.isRenewal() ? "정기 결제 완료 안내" : "구독 결제 완료 안내";
    String htmlContent =
        SubscriptionEmailTemplate.getPaymentSuccessHtmlBase64(
            name,
            planName,
            event.price(),
            event.paidAt(),
            event.nextBillingDate(),
            event.receiptUrl());

    sendEmailNotification(email, name, subject, htmlContent);

    String title = event.isRenewal() ? "🔄 Premium 정기 결제 성공" : "🎉 Premium 구독 결제 성공";
    String description =
        String.format(
            "회원: %s (%s)\n결제 ID: %s\n요금제: %s\n결제금액: %,d원\n다음 결제일: %s",
            name,
            email,
            sanitizeText(event.paymentId()),
            planName,
            event.price(),
            event.nextBillingDate() != null ? event.nextBillingDate() : "N/A");

    sendDiscordBillingNotification(title, description);
  }

  @Async("subscriptionEventTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleSubscriptionPaymentFailed(SubscriptionPaymentFailedEvent event) {
    log.info("Handling SubscriptionPaymentFailedEvent for memberId={}", event.memberId());

    String name = sanitizeText(event.memberName());
    String email = sanitizeText(event.memberEmail());
    String planName = sanitizeText(event.planName());
    String failReason =
        sanitizeText(event.failReason() != null ? event.failReason() : "Unknown error");

    sendDiscordBillingNotification(
        "❌ Premium 구독 결제 실패",
        String.format(
            "회원 ID: %s (이메일: %s)\n결제 ID: %s\n실패사유: %s",
            event.memberId(), email, sanitizeText(event.paymentId()), failReason));

    if (event.planName() != null && event.memberEmail() != null) {
      sendEmailNotification(
          email,
          name,
          "정기 결제 실패 안내",
          SubscriptionEmailTemplate.getPaymentFailureHtmlBase64(name, planName, failReason));
    }
  }

  @Async("subscriptionEventTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleSubscriptionCanceled(SubscriptionCanceledEvent event) {
    log.info("Handling SubscriptionCanceledEvent for memberId={}", event.memberId());

    String name = sanitizeText(event.memberName());
    String email = sanitizeText(event.memberEmail());
    String planName = sanitizeText(event.planName());

    sendDiscordBillingNotification(
        "⚠️ Premium 구독 해지 예약",
        String.format(
            "회원 ID: %s (이메일: %s)\n혜택 만료예정일: %s",
            event.memberId(), email, event.endDate() != null ? event.endDate() : "N/A"));

    sendEmailNotification(
        email,
        name,
        "구독 해지 완료 안내",
        SubscriptionEmailTemplate.getCancellationHtmlBase64(name, planName, event.endDate()));
  }

  @Async("subscriptionEventTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleSubscriptionResumed(SubscriptionResumedEvent event) {
    log.info("Handling SubscriptionResumedEvent for memberId={}", event.memberId());

    String name = sanitizeText(event.memberName());
    String email = sanitizeText(event.memberEmail());

    sendDiscordBillingNotification(
        "▶️ Premium 구독 재개",
        String.format(
            "회원: %s (%s)\n다음 결제일: %s",
            name, email, event.nextBillingDate() != null ? event.nextBillingDate() : "N/A"));
  }

  @Async("subscriptionEventTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handlePaymentMethodDeleted(PaymentMethodDeletedEvent event) {
    log.info("Handling PaymentMethodDeletedEvent for memberId={}", event.memberId());

    memberRepository
        .findById(event.memberId())
        .ifPresent(
            member -> {
              String name = sanitizeText(member.getName());
              String email = sanitizeText(member.getEmail());

              sendDiscordBillingNotification(
                  "🗑️ 등록 카드 삭제", String.format("회원: %s (%s)\n결제 카드(빌링키) 삭제 완료", name, email));
            });
  }

  @Async("subscriptionEventTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleSubscriptionExpired(SubscriptionExpiredEvent event) {
    log.info("Handling SubscriptionExpiredEvent for memberId={}", event.memberId());

    String name = sanitizeText(event.memberName());
    String email = sanitizeText(event.memberEmail());
    String planName = sanitizeText(event.planName());

    sendDiscordBillingNotification(
        "ℹ️ Premium 구독 만료", String.format("회원: %s (%s)\n요금제: %s", name, email, planName));

    sendEmailNotification(
        email,
        name,
        "구독 기간 만료 안내",
        SubscriptionEmailTemplate.getSubscriptionExpiredHtmlBase64(
            name, planName, event.endDate()));
  }

  private String sanitizeText(String input) {
    if (input == null) {
      return "";
    }
    return HtmlUtils.htmlEscape(input);
  }

  private void sendEmailNotification(
      String toEmail, String toName, String subject, String htmlContentBase64) {
    SendPulseEmailClient client = sendPulseEmailClientProvider.getIfAvailable();
    if (client == null) {
      log.warn("SendPulseEmailClient not available. Skipping email to {}: {}", toEmail, subject);
      return;
    }

    try {
      SendPulseEmailRequest request =
          SendPulseEmailRequest.builder()
              .email(
                  SendPulseEmailRequest.EmailDetail.builder()
                      .subject(subject)
                      .html(htmlContentBase64)
                      .text(subject)
                      .from(
                          SendPulseEmailRequest.Participant.builder()
                              .name(senderName)
                              .email(senderEmail)
                              .build())
                      .to(
                          List.of(
                              SendPulseEmailRequest.Participant.builder()
                                  .name(toName)
                                  .email(toEmail)
                                  .build()))
                      .build())
              .build();

      client.sendEmail(request);
      log.info("Sent email to {}: {}", toEmail, subject);
    } catch (Exception e) {
      log.error("Failed to send email to {}: {}", toEmail, subject, e);
    }
  }

  private void sendDiscordBillingNotification(String title, String description) {
    if (discordWebhookService == null) {
      log.warn("DiscordWebhookService not available. Skipping billing notification: {}", title);
      return;
    }

    try {
      DiscordWebhookPayload payload =
          DiscordWebhookPayload.builder()
              .embeds(List.of(Embed.builder().title(title).description(description).build()))
              .build();
      discordWebhookService.sendMessageAsync(payload, DiscordChannel.BILLING);
    } catch (Exception e) {
      log.error("Failed to send Discord billing notification: {}", title, e);
    }
  }
}
