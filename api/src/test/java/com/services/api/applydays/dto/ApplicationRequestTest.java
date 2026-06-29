package com.services.api.applydays.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.services.core.applydays.entity.ApplicationChannel;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ApplicationRequestTest {

  @Test
  @DisplayName("ApplicationRequest 생성 시 positionDetail 값의 맨앞/맨뒤 공백을 trim 처리한다")
  void trim_position_detail_in_application_request() {
    // given
    ApplicationRequest request =
        new ApplicationRequest(
            "naver",
            "Naver",
            1L,
            "  Software Engineer  ",
            OffsetDateTime.now(),
            List.of(),
            ApplicationChannel.DIRECT);

    // then
    assertThat(request.positionDetail()).isEqualTo("Software Engineer");
  }

  @Test
  @DisplayName("ApplicationRequest 생성 시 positionDetail이 null이면 NullPointerException이 발생한다")
  void null_position_detail_in_application_request_throws_npe() {
    // when & then
    assertThatThrownBy(
            () ->
                new ApplicationRequest(
                    "naver",
                    "Naver",
                    1L,
                    null,
                    OffsetDateTime.now(),
                    List.of(),
                    ApplicationChannel.DIRECT))
        .isInstanceOf(NullPointerException.class);
  }
}
