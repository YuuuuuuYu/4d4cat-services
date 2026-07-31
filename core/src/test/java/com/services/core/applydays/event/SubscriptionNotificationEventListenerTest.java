package com.services.core.applydays.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.services.core.common.infrastructure.external.sendpulse.SendPulseEmailClient;
import com.services.core.common.infrastructure.external.sendpulse.dto.SendPulseEmailRequest;
import com.services.core.common.notification.discord.DiscordChannel;
import com.services.core.common.notification.discord.DiscordWebhookPayload;
import com.services.core.common.notification.discord.DiscordWebhookService;
import com.services.core.common.persistence.repository.member.MemberRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SubscriptionNotificationEventListenerTest {

  @Mock private ObjectProvider<SendPulseEmailClient> sendPulseEmailClientProvider;
  @Mock private SendPulseEmailClient sendPulseEmailClient;
  @Mock private DiscordWebhookService discordWebhookService;
  @Mock private MemberRepository memberRepository;

  private SubscriptionNotificationEventListener eventListener;

  @BeforeEach
  void setUp() {
    eventListener =
        new SubscriptionNotificationEventListener(
            sendPulseEmailClientProvider, discordWebhookService, memberRepository);
    ReflectionTestUtils.setField(eventListener, "senderEmail", "noreply@applydays.com");
    ReflectionTestUtils.setField(eventListener, "senderName", "ApplyDays");
  }

  @Test
  @DisplayName("SubscriptionPaidEvent 수신 시 이메일 및 디스코드 알림이 발송된다")
  void handleSubscriptionPaid_success() {
    // given
    given(sendPulseEmailClientProvider.getIfAvailable()).willReturn(sendPulseEmailClient);
    SubscriptionPaidEvent event =
        new SubscriptionPaidEvent(
            UUID.randomUUID(),
            "Gildong",
            "gildong@example.com",
            "payment_123",
            "ApplyDays Premium",
            16500L,
            LocalDateTime.now(),
            LocalDate.now().plusMonths(1),
            "https://receipt.url",
            false);

    // when
    eventListener.handleSubscriptionPaid(event);

    // then
    verify(sendPulseEmailClient).sendEmail(any(SendPulseEmailRequest.class));
    verify(discordWebhookService)
        .sendMessageAsync(any(DiscordWebhookPayload.class), eq(DiscordChannel.BILLING));
  }

  @Test
  @DisplayName("SubscriptionCanceledEvent 수신 시 이메일 및 디스코드 알림이 발송된다")
  void handleSubscriptionCanceled_success() {
    // given
    given(sendPulseEmailClientProvider.getIfAvailable()).willReturn(sendPulseEmailClient);
    SubscriptionCanceledEvent event =
        new SubscriptionCanceledEvent(
            UUID.randomUUID(),
            "Gildong",
            "gildong@example.com",
            "ApplyDays Premium",
            LocalDate.now().plusDays(10));

    // when
    eventListener.handleSubscriptionCanceled(event);

    // then
    verify(sendPulseEmailClient).sendEmail(any(SendPulseEmailRequest.class));
    verify(discordWebhookService)
        .sendMessageAsync(any(DiscordWebhookPayload.class), eq(DiscordChannel.BILLING));
  }
}
