package com.services.core.common.notification.email.woorimail;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.woorimail")
public class WoorimailProperties {
  private String serverUrl;
  private String ssl;
  private int sslPort;
  private String authkey;
  private String domain;
  private String type;
  private String mid;
  private String endpoint;
  private String act;
  private String senderEmail;
  private String senderNickname;
  private String wmsDomain;
  private String wmsNick;
}
