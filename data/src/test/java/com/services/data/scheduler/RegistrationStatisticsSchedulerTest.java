package com.services.data.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.services.core.applydays.entity.ApplicationChannel;
import com.services.core.applydays.repository.ApplicationRepository;
import com.services.core.common.notification.discord.DiscordChannel;
import com.services.core.common.notification.discord.DiscordWebhookPayload;
import com.services.core.common.notification.discord.DiscordWebhookService;
import com.services.core.common.persistence.repository.member.MemberRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RegistrationStatisticsSchedulerTest {

  private MemberRepository memberRepository;
  private ApplicationRepository applicationRepository;
  private DiscordWebhookService discordWebhookService;
  private RegistrationStatisticsScheduler statisticsScheduler;

  @BeforeEach
  void setUp() {
    memberRepository = mock(MemberRepository.class);
    applicationRepository = mock(ApplicationRepository.class);
    discordWebhookService = mock(DiscordWebhookService.class);
    statisticsScheduler =
        new RegistrationStatisticsScheduler(
            memberRepository, applicationRepository, discordWebhookService);
  }

  @Test
  @DisplayName("일간 플랫폼 통계 스케줄러가 정상 동작하고 지정된 STATISTICS 채널로 메시지를 전송하는지 검증")
  void sendDailyStatistics_shouldAggregateAndSendNotification() {
    // Given
    when(memberRepository.countByCreatedAtBetween(any(), any())).thenReturn(5L);
    when(applicationRepository.countByCreatedAtBetween(any(), any())).thenReturn(10L);

    Object[] directStat = new Object[] {ApplicationChannel.DIRECT, 6L};
    Object[] wantedStat = new Object[] {ApplicationChannel.WANTED, 4L};
    when(applicationRepository.countByChannelAndCreatedAtBetween(any(), any()))
        .thenReturn(List.of(directStat, wantedStat));

    // When
    statisticsScheduler.sendDailyStatistics();

    // Then
    verify(memberRepository).countByCreatedAtBetween(any(), any());
    verify(applicationRepository).countByCreatedAtBetween(any(), any());
    verify(applicationRepository).countByChannelAndCreatedAtBetween(any(), any());

    ArgumentCaptor<DiscordWebhookPayload> payloadCaptor =
        ArgumentCaptor.forClass(DiscordWebhookPayload.class);
    verify(discordWebhookService)
        .sendMessageAsync(payloadCaptor.capture(), eq(DiscordChannel.STATISTICS));

    DiscordWebhookPayload payload = payloadCaptor.getValue();
    org.junit.jupiter.api.Assertions.assertNotNull(payload);
    org.junit.jupiter.api.Assertions.assertEquals("Platform Statistics Bot", payload.getUsername());
    org.junit.jupiter.api.Assertions.assertFalse(payload.getEmbeds().isEmpty());
    org.junit.jupiter.api.Assertions.assertTrue(
        payload.getEmbeds().get(0).getDescription().contains("신규 가입자 수:** 5명"));
    org.junit.jupiter.api.Assertions.assertTrue(
        payload.getEmbeds().get(0).getDescription().contains("DIRECT: 6건"));
  }
}
