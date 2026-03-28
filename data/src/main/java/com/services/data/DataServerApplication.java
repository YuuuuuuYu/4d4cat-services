package com.services.data;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.services.data", "com.services.core"})
public class DataServerApplication {

  public static void main(String[] args) {
    SpringApplication.run(DataServerApplication.class, args);
  }
}
