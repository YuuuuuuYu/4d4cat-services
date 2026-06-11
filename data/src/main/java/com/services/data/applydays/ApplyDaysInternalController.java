package com.services.data.applydays;

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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/internal/applydays")
@RequiredArgsConstructor
public class ApplyDaysInternalController {

  private final JobLauncher jobLauncher;
  private final JobExplorer jobExplorer;
  private final Job applyDaysAggregationJob;
  private final CacheManager cacheManager;

  @PostMapping("/statistics/refresh")
  public String refreshStatistics() {
    log.info("Manual trigger: Starting statistics refresh job.");

    // Check if the job is already running to avoid concurrent execution issues
    Set<JobExecution> runningJobs =
        jobExplorer.findRunningJobExecutions(applyDaysAggregationJob.getName());

    if (!runningJobs.isEmpty()) {
      log.warn("Manual trigger: Statistics job is already running.");
      return "Statistics refresh job is already in progress.";
    }

    try {
      // Clear cache before starting to resolve any existing serialization issues
      clearCache();

      JobParameters jobParameters =
          new JobParametersBuilder().addLong("time", System.currentTimeMillis()).toJobParameters();

      jobLauncher.run(applyDaysAggregationJob, jobParameters);

      log.info("Manual trigger: Statistics refresh job started successfully.");
      return "Statistics refresh job started successfully";
    } catch (Exception e) {
      log.error("Manual trigger: Failed to start statistics refresh job", e);
      return "Failed to start statistics refresh job: " + e.getMessage();
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
