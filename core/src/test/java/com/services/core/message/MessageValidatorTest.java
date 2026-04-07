package com.services.core.message;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class MessageValidatorTest {

  @ParameterizedTest
  @CsvSource(
      value = {
        "안녕하세요, true",
        "Hello, true",
        "안녕하세요 Hello 123, true",
        "Special!@#, false",
        " , false",
        "null, false"
      },
      nullValues = {"null"})
  @DisplayName("isValid - 유효한 문자 및 패턴 검증")
  void isValid_shouldValidateContent(String content, boolean expected) {
    // When
    boolean result = MessageValidator.isValid(content);

    // Then
    assertThat(result).isEqualTo(expected);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "이것은 서른 자가 넘는 아주 긴 메시지입니다. 한글은 두 자로 계산되기 때문에 금방 초과하게 됩니다.",
        "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" // 31 chars
      })
  @DisplayName("isValid - 최대 글자 수 초과 시 false 반환")
  void isValid_whenExceedsMaxCharCount_shouldReturnFalse(String content) {
    // When
    boolean result = MessageValidator.isValid(content);

    // Then
    assertThat(result).isFalse();
  }

  @ParameterizedTest
  @CsvSource({"한글, 4", "abc, 3", "한글abc, 7"})
  @DisplayName("calculateCharCount - 한글은 2자, 나머지는 1자로 계산")
  void calculateCharCount_shouldCalculateCorrectly(String text, int expected) {
    // When
    int result = MessageValidator.calculateCharCount(text);

    // Then
    assertThat(result).isEqualTo(expected);
  }
}
