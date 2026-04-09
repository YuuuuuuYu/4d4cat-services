package com.services.data.config;

import static org.mockito.Mockito.mock;

import com.services.core.infrastructure.RedisDataStorage;
import com.services.core.infrastructure.RedisMessageStorage;
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
}
