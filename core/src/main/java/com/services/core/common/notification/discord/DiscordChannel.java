package com.services.core.common.notification.discord;

import lombok.Getter;

@Getter
public enum DiscordChannel {
  DEFAULT("default"),
  DATA("data"),
  MONITORING("monitoring"),
  STATISTICS("statistics");

  private final String value;

  DiscordChannel(String value) {
    this.value = value;
  }
}
