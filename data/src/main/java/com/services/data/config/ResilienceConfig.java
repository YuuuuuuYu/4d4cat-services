package com.services.data.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResilienceConfig {

  @Bean
  public RateLimiter sendPulseRateLimiter() {
    RateLimiterConfig config =
        RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofMinutes(1))
            .limitForPeriod(950) // Safe buffer for 1000/min
            .timeoutDuration(Duration.ofSeconds(10))
            .build();

    RateLimiterRegistry registry = RateLimiterRegistry.of(config);
    return registry.rateLimiter("sendPulseRateLimiter");
  }
}
