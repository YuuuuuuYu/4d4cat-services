package com.services.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import java.time.Duration;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

  @Value("${spring.data.redis.host:localhost}")
  private String host;

  @Value("${spring.data.redis.port:6379}")
  private int port;

  @Bean
  public RedisConnectionFactory redisConnectionFactory() {
    // 1. Connection Pool 설정
    GenericObjectPoolConfig<Object> poolConfig = new GenericObjectPoolConfig<>();
    poolConfig.setMaxTotal(20); // 최대 연결 수
    poolConfig.setMaxIdle(10); // 최대 유휴 연결 수
    poolConfig.setMinIdle(5); // 최소 유휴 연결 수
    poolConfig.setTestOnBorrow(true); // 연결 사용 전 유효성 검사
    poolConfig.setTestWhileIdle(true); // 유휴 연결 주기적 검사
    poolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(30)); // 30초마다 유휴 연결 정리

    // 2. Socket 옵션 (Connect Timeout)
    SocketOptions socketOptions =
        SocketOptions.builder()
            .connectTimeout(Duration.ofMillis(2000)) // 연결 시도 최대 2초
            .build();

    // 3. Client 옵션
    ClientOptions clientOptions =
        ClientOptions.builder()
            .socketOptions(socketOptions)
            .disconnectedBehavior(
                ClientOptions.DisconnectedBehavior.REJECT_COMMANDS) // 연결 끊김 시 명령 거부
            .build();

    // 4. Lettuce Client 설정 (Command Timeout + Pool)
    LettucePoolingClientConfiguration clientConfig =
        LettucePoolingClientConfiguration.builder()
            .commandTimeout(Duration.ofMillis(3000)) // 명령 응답 대기 최대 3초
            .poolConfig(poolConfig)
            .clientOptions(clientOptions)
            .build();

    // 5. Redis 서버 설정
    RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration(host, port);

    return new LettuceConnectionFactory(redisConfig, clientConfig);
  }

  @Bean
  public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);

    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    objectMapper.activateDefaultTyping(
        objectMapper.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.NON_FINAL);

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
