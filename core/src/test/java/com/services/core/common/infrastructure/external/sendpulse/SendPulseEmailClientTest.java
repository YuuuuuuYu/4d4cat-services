package com.services.core.common.infrastructure.external.sendpulse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

class SendPulseEmailClientTest {

  @Test
  @DisplayName("SendPulseEmailClient 기본 생성자가 정상적으로 RestClient를 초기화한다")
  void sendPulseEmailClient_defaultConstructor_success() {
    // when & then
    assertThatCode(
            () -> {
              SendPulseEmailClient client = new SendPulseEmailClient();
              RestClient restClient =
                  (RestClient) ReflectionTestUtils.getField(client, "restClient");
              assertThat(restClient).isNotNull();
            })
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("SendPulseEmailClient Custom RestClient 주입이 정상 동작한다")
  void sendPulseEmailClient_customRestClient_success() {
    // given
    RestClient mockRestClient = RestClient.create();

    // when
    SendPulseEmailClient client = new SendPulseEmailClient(mockRestClient);
    RestClient restClient = (RestClient) ReflectionTestUtils.getField(client, "restClient");

    // then
    assertThat(restClient).isSameAs(mockRestClient);
  }
}
