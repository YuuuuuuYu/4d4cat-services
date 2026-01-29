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
    redisTemplate.delete(key);
    if (data != null && !data.isEmpty()) {
      redisTemplate.opsForList().rightPushAll(key, data.toArray());
      log.info("Stored {} items to Redis key: {}", data.size(), key);
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

  public <T> Optional<T> getRandomElement(String key, Class<T> elementType) {
    List<T> list = getListData(key, elementType);
    if (list.isEmpty()) {
      return Optional.empty();
    }
    int randomIndex = RandomUtils.generateRandomInt(list.size());
    return Optional.of(list.get(randomIndex));
  }

  public <T> T getRandomElement(String key, Class<T> elementType, ErrorCode errorCode) {
    return getRandomElement(key, elementType).orElseThrow(() -> new NotFoundException(errorCode));
  }

  public Long getListSize(String key) {
    return redisTemplate.opsForList().size(key);
  }
}
