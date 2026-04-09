package com.services.api.techblog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.services.api.techblog.dto.TechBlogListResponse;
import com.services.core.infrastructure.RedisDataStorage;
import com.services.core.techblog.repository.TechBlogPostStatRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TechBlogQueryServiceUnitTest {

  @Mock private TechBlogPostStatRepository statRepository;
  @Mock private RedisDataStorage redisDataStorage;

  @InjectMocks private TechBlogQueryService techBlogQueryService;

  @Test
  @DisplayName("incrementClickCount - 클릭수 증가 호출 검증")
  void incrementClickCount_shouldInvokeRepository() {
    // Given
    Long postId = 1L;

    // When
    techBlogQueryService.incrementClickCount(postId);

    // Then
    verify(statRepository, times(1)).incrementClickCount(postId);
  }

  @Test
  @DisplayName("getTechBlogs - 캐시 히트 시 DB 조회 없이 캐시 데이터 반환")
  void getTechBlogs_whenCacheHit_shouldReturnCachedData() {
    // Given
    TechBlogListResponse cachedResponse = new TechBlogListResponse(List.of(), null, false);
    when(redisDataStorage.getCache(anyString())).thenReturn(Optional.of(cachedResponse));

    // When
    TechBlogListResponse result = techBlogQueryService.getTechBlogs(null, null, null);

    // Then
    assertThat(result).isEqualTo(cachedResponse);
    verify(redisDataStorage, never()).setCache(anyString(), any(), anyLong(), any());
  }

  @Test
  @DisplayName("getTechBlogs - 캐시 미스 시 캐시 확인 로그 검증")
  void getTechBlogs_whenCacheMiss_shouldCheckCache() {
    // Given
    when(redisDataStorage.getCache(anyString())).thenReturn(Optional.empty());

    // When & Then
    try {
      techBlogQueryService.getTechBlogs(null, null, null);
    } catch (Exception ignored) {
    }

    verify(redisDataStorage, times(1)).getCache(anyString());
  }
}
