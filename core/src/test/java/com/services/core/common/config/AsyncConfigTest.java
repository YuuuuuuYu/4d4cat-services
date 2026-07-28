package com.services.core.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.Executor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

class AsyncConfigTest {

  @Test
  @DisplayName(
      "subscriptionEventTaskExecutor가 Virtual Threads 기반 SimpleAsyncTaskExecutor로 올바르게 설정되는지 검증한다")
  void testSubscriptionEventTaskExecutor() {
    // given
    AsyncConfig asyncConfig = new AsyncConfig();

    // when
    Executor executor = asyncConfig.subscriptionEventTaskExecutor();

    // then
    assertThat(executor).isInstanceOf(SimpleAsyncTaskExecutor.class);

    SimpleAsyncTaskExecutor taskExecutor = (SimpleAsyncTaskExecutor) executor;
    assertThat(taskExecutor.getThreadNamePrefix()).isEqualTo("Sub-Async-");
  }

  @Test
  @DisplayName("기본 AsyncExecutor가 Virtual Threads 기반 SimpleAsyncTaskExecutor로 올바르게 설정되는지 검증한다")
  void testGetAsyncExecutor() {
    // given
    AsyncConfig asyncConfig = new AsyncConfig();

    // when
    Executor executor = asyncConfig.getAsyncExecutor();

    // then
    assertThat(executor).isInstanceOf(SimpleAsyncTaskExecutor.class);

    SimpleAsyncTaskExecutor taskExecutor = (SimpleAsyncTaskExecutor) executor;
    assertThat(taskExecutor.getThreadNamePrefix()).isEqualTo("Async-Notification-");
  }
}
