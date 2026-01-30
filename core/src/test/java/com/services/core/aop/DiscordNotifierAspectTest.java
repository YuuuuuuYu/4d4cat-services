package com.services.core.aop;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.services.core.notification.discord.DiscordWebhookService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Component;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(
    classes = {DiscordNotifierAspectTest.TestComponent.class, DiscordNotifierAspect.class})
@EnableAspectJAutoProxy
@ExtendWith(MockitoExtension.class)
class DiscordNotifierAspectTest {

  @Component
  static class TestComponent {
    @NotifyDiscord(taskName = "테스트 작업")
    public void annotatedMethod() {
      // Method body is not important for this test
    }
  }

  @Autowired private TestComponent testComponent;

  @MockitoBean private DiscordWebhookService discordWebhookService;

  @Test
  @DisplayName("@NotifyDiscord 어노테이션이 붙은 메서드가 호출되면 Aspect가 동작하여 Discord 알림을 보낸다")
  void aspectShouldTriggerForAnnotatedMethod() {
    // Given & When
    testComponent.annotatedMethod();

    // Then
    // Verify that the async message sending method was called exactly once.
    verify(discordWebhookService, times(1)).sendMessageAsync(any());
  }
}
