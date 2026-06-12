package com.services.core.common.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.ClientOptions.DisconnectedBehavior;
import io.lettuce.core.SocketOptions;
import java.time.Duration;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

  @Value("${spring.data.redis.host:localhost}")
  private String host;

  @Value("${spring.data.redis.port:6379}")
  private int port;

  @Bean
  public RedisCacheConfiguration redisCacheConfiguration() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // NON_FINAL typing with As.PROPERTY is stable for simple POJOs
    objectMapper.activateDefaultTyping(
        objectMapper.getPolymorphicTypeValidator(), DefaultTyping.NON_FINAL, As.PROPERTY);

    GenericJackson2JsonRedisSerializer jsonSerializer =
        new GenericJackson2JsonRedisSerializer(objectMapper);

    return RedisCacheConfiguration.defaultCacheConfig()
        .serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()))
        .serializeValuesWith(SerializationPair.fromSerializer(jsonSerializer))
        .entryTtl(Duration.ofDays(1));
  }

  @Bean
  @ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
  public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
    return RedisCacheManager.builder(connectionFactory)
        .cacheDefaults(redisCacheConfiguration())
        .build();
  }

  @Bean
  public RedisConnectionFactory redisConnectionFactory() {
    // 1. Connection Pool 설정
    GenericObjectPoolConfig<Object> poolConfig = new GenericObjectPoolConfig<>();
    poolConfig.setMaxTotal(32);
    poolConfig.setMaxIdle(16);
    poolConfig.setMinIdle(8);
    poolConfig.setTestOnBorrow(false); // 성능 최적화를 위해 false로 설정 (순간 부하 시 PING 오버헤드 방지)
    poolConfig.setTestWhileIdle(true);
    poolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(30));
    poolConfig.setMaxWait(Duration.ofSeconds(2)); // 커넥션 획득 대기 시간 제한 (무한 대기 방지)

    // 2. Socket 옵션
    SocketOptions socketOptions =
        SocketOptions.builder().connectTimeout(Duration.ofSeconds(5)).build();

    // 3. Client 옵션
    ClientOptions clientOptions =
        ClientOptions.builder()
            .socketOptions(socketOptions)
            .disconnectedBehavior(DisconnectedBehavior.REJECT_COMMANDS)
            .build();

    // 4. Lettuce Client 설정
    LettucePoolingClientConfiguration clientConfig =
        LettucePoolingClientConfiguration.builder()
            .commandTimeout(Duration.ofSeconds(10)) // 블로킹 팝(5초) 등 긴 작업을 고려하여 10초로 설정
            .poolConfig(poolConfig)
            .clientOptions(clientOptions)
            .build();

    // 5. Redis 서버 설정
    RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration(host, port);

    LettuceConnectionFactory factory = new LettuceConnectionFactory(redisConfig, clientConfig);
    factory.setShareNativeConnection(true);
    factory.setValidateConnection(false); // 팩토리 수준의 유효성 검사 비활성화로 성능 최적화
    return factory;
  }

  @Bean
  public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);

    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    objectMapper.activateDefaultTyping(
        objectMapper.getPolymorphicTypeValidator(), DefaultTyping.NON_FINAL, As.PROPERTY);

    GenericJackson2JsonRedisSerializer jsonSerializer =
        new GenericJackson2JsonRedisSerializer(objectMapper);

    template.setKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(jsonSerializer);
    template.setHashKeySerializer(new StringRedisSerializer());
    template.setHashValueSerializer(jsonSerializer);

    template.afterPropertiesSet();
    return template;
  }
}
