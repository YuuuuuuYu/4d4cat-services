package com.services.core.applydays.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.services.core.fixture.ApplyDaysFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ApplicationTest {

  @Test
  @DisplayName("approve 호출 시 verificationStatus가 APPROVED로 변경된다.")
  void approve_sets_status_to_approved() {
    // given
    Application application = ApplyDaysFixtures.createApplication("naver", 1L);

    // when
    application.approve();

    // then
    assertThat(application.getVerificationStatus()).isEqualTo(VerificationStatus.APPROVED);
  }

  @Test
  @DisplayName("생성 시 상세직군(positionDetail)의 맨앞/맨뒤 공백을 trim 처리한다.")
  void trim_position_detail_on_creation() {
    // given
    Application application = Application.builder().positionDetail("  Backend Developer  ").build();

    // then
    assertThat(application.getPositionDetail()).isEqualTo("Backend Developer");
  }

  @Test
  @DisplayName("수정 시 상세직군(positionDetail)의 맨앞/맨뒤 공백을 trim 처리한다.")
  void trim_position_detail_on_update() {
    // given
    Application application = Application.builder().positionDetail("Backend Developer").build();

    // when
    application.update("naver", 1L, "  Frontend Developer  ", null, null, null);

    // then
    assertThat(application.getPositionDetail()).isEqualTo("Frontend Developer");
  }
}
