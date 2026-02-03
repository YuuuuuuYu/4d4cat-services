package com.services.core.infrastructure;

import com.services.core.exception.ErrorCode;
import com.services.core.exception.NotFoundException;
import com.services.core.util.RandomUtils;
import java.util.List;
import java.util.Optional;
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

  public <T> void setListData(String key, List<T> data) {
    if (data == null || data.isEmpty()) {
      log.warn("No data to store for key: {}", key);
      return;
    }

    try {
      redisTemplate.executePipelined(createPipelineCallback(key, data));
      log.info("Stored {} items to Redis key: {} (pipeline)", data.size(), key);
    } catch (Exception e) {
      log.error("Failed to store data to Redis key: {}", key, e);
      throw e;
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private <T> RedisCallback<Object> createPipelineCallback(String key, List<T> data) {
    return connection -> {
      RedisSerializer<String> keySerializer = redisTemplate.getStringSerializer();
      RedisSerializer valueSerializer = redisTemplate.getValueSerializer();

      byte[] keyBytes = keySerializer.serialize(key);

      connection.del(keyBytes);

      for (T item : data) {
        byte[] valueBytes = valueSerializer.serialize(item);
        connection.rPush(keyBytes, valueBytes);
      }

      return null;
    };
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
}
