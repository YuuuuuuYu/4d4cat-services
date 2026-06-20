package com.services.data.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.services.core.common.infrastructure.RedisDataStorage;
import com.services.core.common.infrastructure.RedisMessageQueue;
import com.services.core.common.infrastructure.RedisMessageStorage;
import java.util.Optional;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestRedisConfig {

  @Bean
  @Primary
  public RedisDataStorage redisDataStorage() {
    return mock(RedisDataStorage.class);
  }

  @Bean
  @Primary
  public RedisMessageStorage redisMessageStorage() {
    return mock(RedisMessageStorage.class);
  }

  @Bean
  @Primary
  public RedisMessageQueue redisMessageQueue() {
    RedisMessageQueue mockQueue = mock(RedisMessageQueue.class);
    // Stub pop to prevent infinite loop / CPU spinning in background virtual threads
    when(mockQueue.pop(any(), any(), any()))
        .thenAnswer(
            invocation -> {
              Thread.sleep(1000);
              return Optional.empty();
            });
    return mockQueue;
  }
}
