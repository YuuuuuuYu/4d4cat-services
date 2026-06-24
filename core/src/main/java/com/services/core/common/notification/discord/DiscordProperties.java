package com.services.core.common.notification.discord;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "discord")
public class DiscordProperties {
  private Map<String, String> webhooks;
  private Lifecycle lifecycle = new Lifecycle();

  @Getter
  @Setter
  public static class Lifecycle {
    private boolean enabled = false;
  }
}
