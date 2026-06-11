package com.services.data.applydays.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatisticsCacheEvictListener implements JobExecutionListener {

  private final CacheManager cacheManager;

  @Override
  public void afterJob(JobExecution jobExecution) {
    if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
      log.info("ApplyDays aggregation job completed. Evicting statistics caches...");

      evictCache("companyList");
      evictCache("companySearch");
      evictCache("companySummary");
      evictCache("publicSummary");

      log.info("Statistics caches evicted successfully.");
    }
  }

  private void evictCache(String cacheName) {
    try {
      var cache = cacheManager.getCache(cacheName);
      if (cache != null) {
        cache.clear();
        log.debug("Cache '{}' cleared.", cacheName);
      } else {
        log.warn("Cache '{}' not found during eviction.", cacheName);
      }
    } catch (Exception e) {
      log.error(
          "Failed to evict cache '{}' after job due to Redis issue: {}", cacheName, e.getMessage());
    }
  }
}
