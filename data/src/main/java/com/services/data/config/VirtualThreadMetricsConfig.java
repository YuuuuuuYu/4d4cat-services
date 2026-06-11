package com.services.data.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import java.lang.management.ManagementFactory;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VirtualThreadMetricsConfig {

  private static final Logger log = LoggerFactory.getLogger(VirtualThreadMetricsConfig.class);

  @Bean
  public MeterBinder virtualThreadMeterBinder() {
    return registry -> {
      try {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        ObjectName objectName = new ObjectName("java.lang:type=Threading");

        if (mBeanServer.isRegistered(objectName)) {
          Gauge.builder(
                  "jvm.threads.virtual.count",
                  mBeanServer,
                  server -> {
                    try {
                      return ((Number) server.getAttribute(objectName, "VirtualThreadCount"))
                          .doubleValue();
                    } catch (Exception e) {
                      return 0.0;
                    }
                  })
              .description("The current number of active virtual threads")
              .register(registry);

          log.info("Successfully registered VirtualThreadCount metric to Micrometer.");
        } else {
          log.warn(
              "java.lang:type=Threading MBean is not registered. Virtual thread metrics will not be available.");
        }
      } catch (Exception e) {
        log.error("Failed to register Virtual Thread metrics", e);
      }
    };
  }
}
