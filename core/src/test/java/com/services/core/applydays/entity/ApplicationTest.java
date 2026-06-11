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
}
