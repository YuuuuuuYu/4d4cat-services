package com.services.core.aop;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.services.core.notification.discord.DiscordWebhookService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Component;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@SpringBootTest(
    classes = {
      DiscordNotifierAspectTest.TestComponent.class,
      DiscordNotifierAspect.class,
      DiscordNotifierAspectTest.TestConfig.class
    })
@EnableAspectJAutoProxy
class DiscordNotifierAspectTest {

  @Configuration
  static class TestConfig {
    @Bean
    public MeterRegistry meterRegistry() {
      return new SimpleMeterRegistry();
    }
  }

  @Component
  static class TestComponent {
    @NotifyDiscord(taskName = "테스트 작업")
    public void annotatedMethod() {
      // Method body is not important for this test
    }
  }

  @Autowired private TestComponent testComponent;

  @MockitoBean private DiscordWebhookService discordWebhookService;

  @Autowired private MeterRegistry meterRegistry;

  @Test
  @DisplayName("어노테이션 메서드 호출 시 Discord 알림 전송 - 성공")
  void annotatedMethod_shouldTriggerDiscordNotification() {
    // Given & When
    testComponent.annotatedMethod();

    // Then
    verify(discordWebhookService).sendMessageAsync(any());
  }
}
