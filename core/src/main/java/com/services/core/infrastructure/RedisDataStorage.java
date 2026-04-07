package com.services.core.infrastructure;

import com.services.core.exception.ErrorCode;
import com.services.core.exception.NotFoundException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisDataStorage {

  private final RedisTemplate<String, Object> redisTemplate;
  private final MeterRegistry registry;

  public <T> void setData(String key, Collection<T> data) {
    if (data == null || data.isEmpty()) {
      log.warn("No data to store for key: {}", key);
      return;
    }

    Timer.builder("redis.pipeline.duration")
        .tag("key", key)
        .register(registry)
        .record(
            () -> {
              try {
                redisTemplate.executePipelined(createPipelineCallback(key, data));
                log.info("Stored {} items to Redis key: {} (pipeline set)", data.size(), key);
              } catch (Exception e) {
                log.error("Failed to store data to Redis key: {}", key, e);
                throw e;
              }
            });
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private <T> RedisCallback<Object> createPipelineCallback(String key, Collection<T> data) {
    return connection -> {
      RedisSerializer<String> keySerializer = redisTemplate.getStringSerializer();
      RedisSerializer valueSerializer = redisTemplate.getValueSerializer();

      byte[] keyBytes = keySerializer.serialize(key);

      connection.del(keyBytes);

      for (T item : data) {
        byte[] valueBytes = valueSerializer.serialize(item);
        connection.sAdd(keyBytes, valueBytes);
      }

      return null;
    };
  }

  @SuppressWarnings("unchecked")
  public <T> Optional<T> getRandomElement(String key, Class<T> elementType) {
    Object element = redisTemplate.opsForSet().randomMember(key);

    String status = (element == null) ? "miss" : "hit";
    registry.counter("redis.random.access", "key", key, "status", status).increment();

    return element == null ? Optional.empty() : Optional.of((T) element);
  }

  public <T> T getRandomElement(String key, Class<T> elementType, ErrorCode errorCode) {
    return getRandomElement(key, elementType).orElseThrow(() -> new NotFoundException(errorCode));
  }

  /**
   * 일반 객체를 Redis에 캐싱합니다.
   *
   * @param key 캐시 키
   * @param value 저장할 객체
   * @param timeout 만료 시간
   * @param unit 시간 단위
   */
  public <T> void setCache(String key, T value, long timeout, TimeUnit unit) {
    try {
      redisTemplate.opsForValue().set(key, value, timeout, unit);
      log.debug("Cached data to Redis. key: {}, timeout: {} {}", key, timeout, unit);
    } catch (Exception e) {
      log.error("Failed to set cache in Redis. key: {}", key, e);
    }
  }

  /**
   * Redis에서 캐싱된 객체를 가져옵니다.
   *
   * @param key 캐시 키
   * @return 캐싱된 객체 (Optional)
   */
  @SuppressWarnings("unchecked")
  public <T> Optional<T> getCache(String key) {
    try {
      Object value = redisTemplate.opsForValue().get(key);
      return Optional.ofNullable((T) value);
    } catch (Exception e) {
      log.error("Failed to get cache from Redis. key: {}", key, e);
      return Optional.empty();
    }
  }

  /**
   * 패턴에 매칭되는 모든 키를 삭제합니다.
   *
   * @param pattern 삭제할 키 패턴 (예: "techblog:list:*")
   */
  public void deleteKeysByPattern(String pattern) {
    try {
      Set<String> keys = redisTemplate.keys(pattern);
      if (keys != null && !keys.isEmpty()) {
        redisTemplate.delete(keys);
        log.info("Deleted {} keys matching pattern: {}", keys.size(), pattern);
      }
    } catch (Exception e) {
      log.error("Failed to delete keys by pattern: {}", pattern, e);
    }
  }
}
