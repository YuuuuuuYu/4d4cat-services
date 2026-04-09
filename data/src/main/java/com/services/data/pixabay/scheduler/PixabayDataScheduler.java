package com.services.data.pixabay.scheduler;

import com.services.data.pixabay.PixabayMusicCollector;
import com.services.data.pixabay.PixabayVideoCollector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PixabayDataScheduler {

  private final PixabayVideoCollector videoCollector;
  private final PixabayMusicCollector musicCollector;

  @EventListener(ApplicationReadyEvent.class)
  public void initializeData() {
    Thread.startVirtualThread(
        () -> {
          log.info("=== Starting initial data collection in virtual thread ===");
          collectAllData();
          log.info("=== Initial data collection completed ===");
        });
  }

  @Scheduled(cron = "0 0 3 * * *")
  public void scheduledDailyCollection() {
    log.info("=== Starting scheduled daily data collection (3 AM) ===");
    collectAllData();
    log.info("=== Scheduled daily data collection completed ===");
  }

  private void collectAllData() {
    try {
      videoCollector.collectAndStore();
    } catch (Exception e) {
      log.error("Failed to collect video data: {}", e.getMessage(), e);
    }

    try {
      musicCollector.collectAndStore();
    } catch (Exception e) {
      log.error("Failed to collect music data: {}", e.getMessage(), e);
    }
  }
}
