package com.services.data.scheduler;

import com.services.core.applydays.entity.ApplicationChannel;
import com.services.core.applydays.repository.ApplicationRepository;
import com.services.core.common.notification.discord.DiscordChannel;
import com.services.core.common.notification.discord.DiscordWebhookPayload;
import com.services.core.common.notification.discord.DiscordWebhookService;
import com.services.core.common.notification.discord.Embed;
import com.services.core.common.persistence.repository.member.MemberRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RegistrationStatisticsScheduler {

  private final MemberRepository memberRepository;
  private final ApplicationRepository applicationRepository;
  private final DiscordWebhookService discordWebhookService;

  private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");
  private static final String BOT_USERNAME = "Platform Statistics Bot";
  private static final int DISCORD_COLOR_INFO = 3447003; // Soft Blue

  @Scheduled(cron = "0 0 1 * * *", zone = "Asia/Seoul")
  public void sendDailyStatistics() {
    LocalDate yesterday = LocalDate.now(KST_ZONE).minusDays(1);
    LocalDateTime start = yesterday.atStartOfDay();
    LocalDateTime end = yesterday.atTime(LocalTime.MAX);

    log.info("Starting daily platform statistics aggregation for {}", yesterday);

    long newMembers = memberRepository.countByCreatedAtBetween(start, end);
    long newApplications = applicationRepository.countByCreatedAtBetween(start, end);
    List<Object[]> channelStats =
        applicationRepository.countByChannelAndCreatedAtBetween(start, end);

    String title =
        String.format("📊 [일간 플랫폼 통계] - %s", yesterday.format(DateTimeFormatter.ISO_LOCAL_DATE));
    String description = buildStatisticsDescription(newMembers, newApplications, channelStats);

    sendDiscordNotification(title, description);
  }

  @Scheduled(cron = "0 0 7 * * MON", zone = "Asia/Seoul")
  public void sendWeeklyStatistics() {
    LocalDate today = LocalDate.now(KST_ZONE);
    LocalDate endDay = today.minusDays(1);
    LocalDate startDay = today.minusDays(7);

    LocalDateTime start = startDay.atStartOfDay();
    LocalDateTime end = endDay.atTime(LocalTime.MAX);

    log.info("Starting weekly platform statistics aggregation from {} to {}", startDay, endDay);

    long newMembers = memberRepository.countByCreatedAtBetween(start, end);
    long newApplications = applicationRepository.countByCreatedAtBetween(start, end);
    List<Object[]> channelStats =
        applicationRepository.countByChannelAndCreatedAtBetween(start, end);

    String title =
        String.format(
            "📊 [주간 플랫폼 통계] - %s ~ %s",
            startDay.format(DateTimeFormatter.ISO_LOCAL_DATE),
            endDay.format(DateTimeFormatter.ISO_LOCAL_DATE));
    String description = buildStatisticsDescription(newMembers, newApplications, channelStats);

    sendDiscordNotification(title, description);
  }

  private String buildStatisticsDescription(
      long newMembers, long newApplications, List<Object[]> channelStats) {
    StringBuilder sb = new StringBuilder();
    sb.append(String.format("• **신규 가입자 수:** %d명\n", newMembers));
    sb.append(String.format("• **신규 지원서 등록:** %d건\n", newApplications));

    if (newApplications > 0 && channelStats != null && !channelStats.isEmpty()) {
      sb.append("  - **유입 채널별 통계:**\n");
      for (Object[] row : channelStats) {
        ApplicationChannel channel = (ApplicationChannel) row[0];
        long count = (long) row[1];
        sb.append(String.format("    - %s: %d건\n", channel.name(), count));
      }
    }
    return sb.toString();
  }

  private void sendDiscordNotification(String title, String description) {
    Embed embed =
        Embed.builder()
            .title(title)
            .description(description)
            .color(DISCORD_COLOR_INFO)
            .timestamp(LocalDateTime.now(KST_ZONE).toString())
            .build();

    DiscordWebhookPayload payload =
        DiscordWebhookPayload.builder().username(BOT_USERNAME).embeds(List.of(embed)).build();

    discordWebhookService.sendMessageAsync(payload, DiscordChannel.STATISTICS);
  }
}
