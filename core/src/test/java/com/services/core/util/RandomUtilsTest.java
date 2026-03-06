package com.services.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RandomUtilsTest {

  @Test
  @DisplayName("generateRandomInt - 0보다 크거나 같고 max보다 작은 값 반환")
  void generateRandomInt_shouldReturnInRange() {
    // Given
    int max = 10;

    // When
    int result = RandomUtils.generateRandomInt(max);

    // Then
    assertThat(result).isGreaterThanOrEqualTo(0);
    assertThat(result).isLessThan(max);
  }
}
