package com.services.data.techblog.scheduler;

import com.services.core.infrastructure.RedisDataStorage;
import com.services.core.techblog.repository.TechBlogPostRepository;
import com.services.data.techblog.TechBlogRssCollector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class TechBlogDataScheduler {

  private final TechBlogRssCollector rssCollector;
  private final TechBlogPostRepository postRepository;
  private final RedisDataStorage redisDataStorage;

  @Scheduled(cron = "0 0 8,13,22 * * *")
  public void collectTechBlogFeeds() {
    log.info("Starting scheduled RSS feed collection.");
    rssCollector.collectFeeds();
    log.info("Finished scheduled RSS feed collection. Evicting caches.");
    
    // RSS 수집 완료 후 모든 TechBlog 리스트 캐시 일괄 삭제
    redisDataStorage.deleteKeysByPattern("techblog:list:*");
  }

  @Scheduled(cron = "0 0 0 * * *")
  @Transactional
  public void cleanupUnexposedPosts() {
    log.info("Starting scheduled cleanup of unexposed posts.");
    postRepository.deletePhysicallyAllDeleted();
    log.info("Finished scheduled cleanup of unexposed posts.");
  }
}
