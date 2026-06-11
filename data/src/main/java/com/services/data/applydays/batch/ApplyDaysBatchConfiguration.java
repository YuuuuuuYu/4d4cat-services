package com.services.data.applydays.batch;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ApplyDaysBatchConfiguration {

  @PersistenceContext private final EntityManager entityManager;
  private final StatisticsCacheEvictListener cacheEvictListener;

  @Bean
  public Job applyDaysAggregationJob(JobRepository jobRepository, Step aggregationStep) {
    return new JobBuilder("applyDaysAggregationJob", jobRepository)
        .start(aggregationStep)
        .listener(cacheEvictListener)
        .build();
  }

  @Bean
  public Step aggregationStep(
      JobRepository jobRepository, PlatformTransactionManager transactionManager) {
    return new StepBuilder("aggregationStep", jobRepository)
        .tasklet(
            (contribution, chunkContext) -> {
              log.info("Starting ApplyDays aggregation using PostgreSQL 17.9 JSON_TABLE");

              // 1. 기업 전체 통계, 2. 대분류 통계, 3. 중분류 통계를 각각 집계하여 INSERT
              String sql =
                  """
                  WITH raw_steps AS (
                      SELECT
                          a.id as application_id,
                          a.company_slug,
                          a.category_id,
                          c.parent_id as category_l1_id,
                          jt.step_type,
                          jt.duration_days,
                          jt.status
                      FROM application a
                      INNER JOIN company comp ON a.company_slug = comp.slug
                      INNER JOIN verification_request vr ON a.id = vr.application_id
                      LEFT JOIN category c ON a.category_id = c.id
                      CROSS JOIN JSON_TABLE(
                          a.hiring_process, '$[*]'
                          COLUMNS (
                              step_type TEXT PATH '$.stepType',
                              duration_days INT PATH '$.duration_days',
                              status TEXT PATH '$.status'
                          )
                      ) as jt
                      WHERE a.deleted = false
                        AND comp.status = 'VERIFIED'
                        AND vr.status = 'APPROVED'
                  ),
                  level_stats AS (
                      -- 1. 기업 전체 데이터 구성
                      SELECT
                          company_slug, NULL::bigint as category_id, 'COMPANY' as stat_type,
                          application_id, status, step_type, duration_days
                      FROM raw_steps

                      UNION ALL

                      -- 2. 대분류 데이터 구성
                      SELECT
                          company_slug, category_l1_id as category_id, 'CAT_L1' as stat_type,
                          application_id, status, step_type, duration_days
                      FROM raw_steps WHERE category_l1_id IS NOT NULL

                      UNION ALL

                      -- 3. 중분류 데이터 구성
                      SELECT
                          company_slug, category_id, 'CAT_L2' as stat_type,
                          application_id, status, step_type, duration_days
                      FROM raw_steps
                  ),
                  summaries AS (
                      -- 각 레벨별 고유 지원서 수 및 무통보 수 집계
                      SELECT
                          company_slug, category_id, stat_type,
                          COUNT(DISTINCT application_id) as review_count,
                          COUNT(DISTINCT application_id) FILTER (WHERE status = 'GHOSTED') as ghosting_count
                      FROM level_stats
                      GROUP BY company_slug, category_id, stat_type
                  ),
                  step_details AS (
                      -- 각 레벨별 전형 단계 상세 통계 집계
                      SELECT
                          company_slug, category_id, stat_type, step_type,
                          COALESCE(ROUND(AVG(duration_days) FILTER (WHERE status != 'GHOSTED'), 1), 0) as avg_days,
                          COUNT(*) FILTER (WHERE status != 'GHOSTED') as step_count
                      FROM level_stats
                      GROUP BY company_slug, category_id, stat_type, step_type
                  ),
                  final_stats AS (
                      -- 집계 데이터 결합 및 JSON 생성
                      SELECT
                          s.company_slug, s.category_id, s.stat_type, s.review_count, s.ghosting_count,
                          jsonb_object_agg(d.step_type, jsonb_build_object('avg', d.avg_days, 'count', d.step_count)) as step_statistics
                      FROM summaries s
                      JOIN step_details d ON s.company_slug = d.company_slug
                          AND (s.category_id IS NOT DISTINCT FROM d.category_id)
                          AND s.stat_type = d.stat_type
                      GROUP BY s.company_slug, s.category_id, s.stat_type, s.review_count, s.ghosting_count
                  )
                  INSERT INTO apply_days_statistics (company_slug, category_id, stat_type, review_count, ghosting_count, step_statistics, updated_at)
                  SELECT company_slug, category_id, stat_type, review_count, ghosting_count, step_statistics, NOW()
                  FROM final_stats
                  ON CONFLICT (company_slug, category_id, stat_type)
                  DO UPDATE SET
                      review_count = EXCLUDED.review_count,
                      ghosting_count = EXCLUDED.ghosting_count,
                      step_statistics = EXCLUDED.step_statistics,
                      updated_at = EXCLUDED.updated_at;
                  """;

              entityManager.createNativeQuery(sql).executeUpdate();

              log.info("ApplyDays aggregation completed successfully");
              return RepeatStatus.FINISHED;
            },
            transactionManager)
        .build();
  }
}
