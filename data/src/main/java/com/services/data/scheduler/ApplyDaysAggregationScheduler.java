package com.services.data.scheduler;

import com.services.core.common.notification.discord.DiscordChannel;
import com.services.core.common.notification.discord.NotifyDiscord;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApplyDaysAggregationScheduler {

  private final JobLauncher jobLauncher;
  private final JobExplorer jobExplorer;
  private final Job applyDaysAggregationJob;
  private final CacheManager cacheManager;

  @Scheduled(cron = "0 10 1 * * *", zone = "Asia/Seoul")
  @NotifyDiscord(taskName = "ApplyDays Statistics Aggregation", channel = DiscordChannel.STATISTICS)
  public void runAggregationJob() {
    log.info("Scheduled trigger: Starting statistics refresh job.");

    Set<JobExecution> runningJobs =
        jobExplorer.findRunningJobExecutions(applyDaysAggregationJob.getName());

    if (!runningJobs.isEmpty()) {
      log.warn("Scheduled trigger: Statistics job is already running.");
      return;
    }

    try {
      clearCache();

      JobParameters jobParameters =
          new JobParametersBuilder().addLong("time", System.currentTimeMillis()).toJobParameters();

      jobLauncher.run(applyDaysAggregationJob, jobParameters);

      log.info("Scheduled trigger: Statistics refresh job started successfully.");
    } catch (Exception e) {
      log.error("Scheduled trigger: Failed to start statistics refresh job", e);
      throw new RuntimeException("Failed to start statistics refresh job", e);
    }
  }

  private void clearCache() {
    try {
      log.info("Evicting statistics caches before refresh...");
      String[] caches = {"companyList", "companySearch", "companySummary", "publicSummary"};
      for (String name : caches) {
        var cache = cacheManager.getCache(name);
        if (cache != null) {
          cache.clear();
        }
      }
    } catch (Exception e) {
      log.warn("Failed to clear caches: {}", e.getMessage());
    }
  }
}
