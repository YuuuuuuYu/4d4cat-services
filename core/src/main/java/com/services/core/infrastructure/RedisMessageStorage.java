package com.services.core.infrastructure;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisMessageStorage {

  private static final String MESSAGE_KEY = "lastMessage";

  private final RedisTemplate<String, Object> redisTemplate;

  public void saveMessage(String content) {
    redisTemplate.opsForValue().set(MESSAGE_KEY, content);
    log.info("Saved message to Redis: {}", content);
  }

  public Optional<String> getMessage() {
    Object value = redisTemplate.opsForValue().get(MESSAGE_KEY);
    return Optional.ofNullable(value).map(Object::toString);
  }
}
