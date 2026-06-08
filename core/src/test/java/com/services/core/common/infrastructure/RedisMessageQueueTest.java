package com.services.core.common.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

@ExtendWith(MockitoExtension.class)
class RedisMessageQueueTest {

  @Mock private RedisTemplate<String, Object> redisTemplate;
  @Mock private ListOperations<String, Object> listOperations;

  private RedisMessageQueue redisMessageQueue;

  @BeforeEach
  void setUp() {
    when(redisTemplate.opsForList()).thenReturn(listOperations);
    redisMessageQueue = new RedisMessageQueue(redisTemplate);
  }

  @Test
  @DisplayName("메시지를 큐의 왼쪽으로 푸시한다")
  void push_Success() {
    // given
    String queueName = "test-queue";
    String message = "hello world";

    // when
    redisMessageQueue.push(queueName, message);

    // then
    verify(listOperations).leftPush(queueName, message);
  }

  @Test
  @DisplayName("큐의 오른쪽에서 메시지를 가져온다 (Blocking Pop)")
  void pop_Success() {
    // given
    String queueName = "test-queue";
    String message = "hello world";
    Duration timeout = Duration.ofSeconds(1);
    when(listOperations.rightPop(eq(queueName), any(Duration.class))).thenReturn(message);

    // when
    Optional<String> result = redisMessageQueue.pop(queueName, timeout, String.class);

    // then
    assertThat(result).isPresent().contains(message);
    verify(listOperations).rightPop(queueName, timeout);
  }

  @Test
  @DisplayName("메시지가 없으면 빈 Optional을 반환한다")
  void pop_Empty() {
    // given
    String queueName = "empty-queue";
    when(listOperations.rightPop(eq(queueName), any(Duration.class))).thenReturn(null);

    // when
    Optional<String> result = redisMessageQueue.pop(queueName, Duration.ofSeconds(1), String.class);

    // then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("팝 도중 예외가 발생하면 예외를 던진다")
  void pop_Exception() {
    // given
    String queueName = "error-queue";
    when(listOperations.rightPop(eq(queueName), any(Duration.class)))
        .thenThrow(new RuntimeException("Redis error"));

    // when & then
    Assertions.assertThrows(
        RuntimeException.class,
        () -> redisMessageQueue.pop(queueName, Duration.ofSeconds(1), String.class));
  }
}
