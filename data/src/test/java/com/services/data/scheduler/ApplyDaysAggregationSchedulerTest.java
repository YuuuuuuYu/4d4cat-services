package com.services.data.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

class ApplyDaysAggregationSchedulerTest {

  private JobLauncher jobLauncher;
  private JobExplorer jobExplorer;
  private Job applyDaysAggregationJob;
  private CacheManager cacheManager;
  private ApplyDaysAggregationScheduler scheduler;

  @BeforeEach
  void setUp() {
    jobLauncher = mock(JobLauncher.class);
    jobExplorer = mock(JobExplorer.class);
    applyDaysAggregationJob = mock(Job.class);
    cacheManager = mock(CacheManager.class);

    scheduler =
        new ApplyDaysAggregationScheduler(
            jobLauncher, jobExplorer, applyDaysAggregationJob, cacheManager);

    when(applyDaysAggregationJob.getName()).thenReturn("applyDaysAggregationJob");
  }

  @Test
  @DisplayName("이미 실행 중인 동일 Job이 없을 때, 캐시를 지우고 배치를 실행한다")
  void runAggregationJob_shouldRunJob_whenNotAlreadyRunning() throws Exception {
    // Given
    when(jobExplorer.findRunningJobExecutions("applyDaysAggregationJob"))
        .thenReturn(Collections.emptySet());

    Cache mockCache = mock(Cache.class);
    when(cacheManager.getCache(anyString())).thenReturn(mockCache);

    // When
    scheduler.runAggregationJob();

    // Then
    verify(mockCache, times(4)).clear();
    verify(jobLauncher).run(eq(applyDaysAggregationJob), any());
  }

  @Test
  @DisplayName("이미 실행 중인 동일 Job이 존재하면 배치를 실행하지 않는다")
  void runAggregationJob_shouldNotRunJob_whenAlreadyRunning() throws Exception {
    // Given
    JobExecution runningJob = mock(JobExecution.class);
    when(jobExplorer.findRunningJobExecutions("applyDaysAggregationJob"))
        .thenReturn(Set.of(runningJob));

    // When
    scheduler.runAggregationJob();

    // Then
    verify(cacheManager, never()).getCache(anyString());
    verify(jobLauncher, never()).run(any(), any());
  }
}
