package com.services.core.common.infrastructure;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisMessageQueue {

  private final RedisTemplate<String, Object> redisTemplate;

  public void push(String queueName, Object message) {
    try {
      redisTemplate.opsForList().leftPush(queueName, message);
      log.debug("Pushed message to Redis queue: {}", queueName);
    } catch (Exception e) {
      log.error("Failed to push message to Redis queue: {}", queueName, e);
      throw e;
    }
  }

  public void zAdd(String queueName, Object message, double score) {
    try {
      redisTemplate.opsForZSet().add(queueName, message, score);
      log.debug("Added message to Redis Sorted Set: {} with score: {}", queueName, score);
    } catch (Exception e) {
      log.error("Failed to add message to Redis Sorted Set: {}", queueName, e);
      throw e;
    }
  }

  @SuppressWarnings("unchecked")
  public <T> List<T> zPopByScore(String queueName, double maxScore) {
    try {
      Set<Object> range = redisTemplate.opsForZSet().rangeByScore(queueName, 0, maxScore);
      if (range == null || range.isEmpty()) {
        return Collections.emptyList();
      }

      List<T> results = (List<T>) range.stream().map(obj -> (T) obj).toList();
      redisTemplate.opsForZSet().remove(queueName, range.toArray());

      return results;
    } catch (Exception e) {
      log.error("Failed to pop messages from Redis Sorted Set: {}", queueName, e);
      return Collections.emptyList();
    }
  }

  @SuppressWarnings("unchecked")
  public <T> Optional<T> pop(String queueName, Duration timeout, Class<T> clazz) {
    try {
      Object message = redisTemplate.opsForList().rightPop(queueName, timeout);
      return Optional.ofNullable((T) message);
    } catch (Exception e) {
      log.error("Failed to pop message from Redis queue: {}", queueName, e);
      throw e;
    }
  }
}
