package com.services.api.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.services.api.config.TestRedisConfig;
import com.services.core.common.exception.ErrorCode;
import java.util.Locale;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
class ErrorCodeMessageMappingTest {

  @Autowired private MessageSource messageSource;

  @Test
  @DisplayName("모든 ErrorCode에 대응하는 메시지가 messages.yml에 정의되어 있어야 한다")
  void verifyAllErrorCodesHaveMessages() {
    for (ErrorCode errorCode : ErrorCode.values()) {
      String messageKey = errorCode.getMessageKey();
      try {
        String message = messageSource.getMessage(messageKey, null, Locale.KOREAN);

        assertThat(message)
            .withFailMessage(
                "ErrorCode %s의 messageKey '%s'가 messages.yml에 정의되어 있지 않습니다.",
                errorCode.name(), messageKey)
            .isNotBlank()
            .isNotEqualTo(messageKey);

      } catch (NoSuchMessageException e) {
        throw new AssertionError(
            String.format(
                "ErrorCode %s의 messageKey '%s'가 messages.yml에 정의되어 있지 않습니다.",
                errorCode.name(), messageKey),
            e);
      }
    }
  }
}
