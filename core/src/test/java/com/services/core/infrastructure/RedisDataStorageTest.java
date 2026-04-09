package com.services.core.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;

@ExtendWith(MockitoExtension.class)
class RedisDataStorageTest {

  @Mock private RedisTemplate<String, Object> redisTemplate;
  @Mock private SetOperations<String, Object> setOperations;
  private MeterRegistry registry;

  private RedisDataStorage storage;

  @BeforeEach
  void setUp() {
    registry = new SimpleMeterRegistry();
    storage = new RedisDataStorage(redisTemplate, registry);
  }

  @Test
  @DisplayName("setData - 데이터가 있을 때 저장 로직 실행")
  void setData_whenDataIsPresent_shouldStoreData() {
    // Given
    String key = "test-key";
    List<String> data = List.of("item1", "item2");

    // When
    storage.setData(key, data);

    // Then
    verify(redisTemplate).executePipelined(any(RedisCallback.class));
    assertThat(registry.find("redis.pipeline.duration").timer()).isNotNull();
  }

  @Test
  @DisplayName("getRandomElement - 데이터가 없을 때 Optional.empty() 반환")
  void getRandomElement_whenNoData_shouldReturnEmpty() {
    // Given
    String key = "test-key";
    when(redisTemplate.opsForSet()).thenReturn(setOperations);
    when(setOperations.randomMember(key)).thenReturn(null);

    // When
    Optional<String> result = storage.getRandomElement(key, String.class);

    // Then
    assertThat(result).isEmpty();
    assertThat(registry.find("redis.random.access").counter().count()).isEqualTo(1.0);
    assertThat(registry.find("redis.random.access").tags("status", "miss").counter()).isNotNull();
  }

  @Test
  @DisplayName("getRandomElement - 데이터가 있을 때 랜덤 요소 반환")
  void getRandomElement_whenDataExists_shouldReturnElement() {
    // Given
    String key = "test-key";
    when(redisTemplate.opsForSet()).thenReturn(setOperations);
    when(setOperations.randomMember(key)).thenReturn("item");

    // When
    Optional<String> result = storage.getRandomElement(key, String.class);

    // Then
    assertThat(result).isPresent();
    assertThat(result.get()).isEqualTo("item");
    assertThat(registry.find("redis.random.access").counter().count()).isEqualTo(1.0);
    assertThat(registry.find("redis.random.access").tags("status", "hit").counter()).isNotNull();
  }
}
