package com.services.core.infrastructure;

import com.services.core.exception.ErrorCode;
import com.services.core.exception.NotFoundException;
import com.services.core.util.RandomUtils;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisDataStorage {

  private final RedisTemplate<String, Object> redisTemplate;

  public <T> void setListData(String key, List<T> data) {
    if (data == null || data.isEmpty()) {
      log.warn("No data to store for key: {}", key);
      return;
    }

    String tempKey = key + ":temp";
    try {
      redisTemplate.delete(tempKey);
      redisTemplate.opsForList().rightPushAll(tempKey, data.toArray());
      redisTemplate.rename(tempKey, key);
      log.info("Stored {} items to Redis key: {} (atomic operation)", data.size(), key);
    } catch (Exception e) {
      log.error("Failed to store data to Redis key: {}", key, e);
      redisTemplate.delete(tempKey);
      throw e;
    }
  }

  @SuppressWarnings("unchecked")
  public <T> List<T> getListData(String key, Class<T> elementType) {
    List<Object> rawList = redisTemplate.opsForList().range(key, 0, -1);
    if (rawList == null || rawList.isEmpty()) {
      return Collections.emptyList();
    }
    return rawList.stream().map(item -> (T) item).toList();
  }

  @SuppressWarnings("unchecked")
  public <T> Optional<T> getRandomElement(String key, Class<T> elementType) {
    Long size = redisTemplate.opsForList().size(key);
    if (size == null || size == 0) {
      return Optional.empty();
    }
    int randomIndex = RandomUtils.generateRandomInt(size.intValue());
    Object element = redisTemplate.opsForList().index(key, randomIndex);
    return element == null ? Optional.empty() : Optional.of((T) element);
  }

  public <T> T getRandomElement(String key, Class<T> elementType, ErrorCode errorCode) {
    return getRandomElement(key, elementType).orElseThrow(() -> new NotFoundException(errorCode));
  }

  public Long getListSize(String key) {
    return redisTemplate.opsForList().size(key);
  }
}
