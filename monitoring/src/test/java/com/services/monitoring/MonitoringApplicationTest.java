package com.services.monitoring;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MonitoringApplicationTest {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  @DisplayName("스프링 컨텍스트 로드 성공")
  void contextLoads_shouldSucceed() {
    // When - Checking if the context loads successfully

    // Then - Succeeds if it reaches here without exception
  }

  @Test
  @DisplayName("Prometheus 메트릭 엔드포인트 조회 - 성공")
  void getPrometheusMetrics_shouldReturnMetrics() {
    // When
    var actuatorResponse = restTemplate.getForEntity("/actuator", String.class);
    System.out.println("Actuator Links: " + actuatorResponse.getBody());

    var response = restTemplate.getForEntity("/actuator/prometheus", String.class);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody()).contains("jvm_memory_used_bytes");
    assertThat(response.getBody()).contains("http_server_requests_seconds_count");
  }

  @Test
  @DisplayName("Health 체크 엔드포인트 조회 - 성공 및 UP 상태 확인")
  void getHealth_shouldReturnUpStatus() {
    // When
    var response = restTemplate.getForEntity("/actuator/health", String.class);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody()).contains("UP");
  }
}
