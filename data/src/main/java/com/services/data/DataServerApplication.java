package com.services.data;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.services.data", "com.services.core"})
@EnableScheduling
public class DataServerApplication {

  public static void main(String[] args) {
    SpringApplication.run(DataServerApplication.class, args);
  }
}
