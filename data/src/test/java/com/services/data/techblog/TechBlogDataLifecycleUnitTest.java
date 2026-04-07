package com.services.data.techblog;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.services.core.infrastructure.RedisDataStorage;
import com.services.core.techblog.repository.TechBlogPostRepository;
import com.services.data.techblog.scheduler.TechBlogDataScheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TechBlogDataLifecycleUnitTest {

  @Mock private TechBlogRssCollector rssCollector;
  @Mock private TechBlogPostRepository postRepository;
  @Mock private RedisDataStorage redisDataStorage;

  @InjectMocks private TechBlogDataScheduler scheduler;

  @Test
  @DisplayName("cleanupUnexposedPosts - 물리적 삭제 호출 검증")
  void cleanupUnexposedPosts_shouldInvokeRepository() {
    // When
    scheduler.cleanupUnexposedPosts();

    // Then
    verify(postRepository, times(1)).deletePhysicallyAllDeleted();
  }

  @Test
  @DisplayName("collectTechBlogFeeds - 피드 수집 및 캐시 삭제 호출 검증")
  void collectTechBlogFeeds_shouldInvokeCollectorAndEvictCache() {
    // When
    scheduler.collectTechBlogFeeds();

    // Then
    verify(rssCollector, times(1)).collectFeeds();
    verify(redisDataStorage, times(1)).deleteKeysByPattern("techblog:list:*");
  }
}
