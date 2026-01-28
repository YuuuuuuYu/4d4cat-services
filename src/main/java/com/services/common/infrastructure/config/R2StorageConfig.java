package com.services.common.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

//@Configuration
public class R2StorageConfig {

  @Value("${cloudflare.r2.access-key}")
  private String accessKey;

  @Value("${cloudflare.r2.secret-key}")
  private String secretKey;

  @Value("${cloudflare.r2.endpoint}")
  private String endpointUrl;

  @Bean
  public S3Client s3Client() {
    return S3Client.builder()
        .endpointOverride(URI.create(endpointUrl))
        .credentialsProvider(StaticCredentialsProvider.create(
            AwsBasicCredentials.create(accessKey, secretKey)
        ))
        .build();
  }
}
