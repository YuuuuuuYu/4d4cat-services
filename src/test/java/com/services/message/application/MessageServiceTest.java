package com.services.message.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.services.common.application.exception.BadRequestException;
import com.services.common.application.exception.ErrorCode;
import com.services.message.presentation.dto.MessageRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

  @InjectMocks private MessageService messageService;

  @Test
  @DisplayName("saveMessage - 유효한 메시지 저장 성공")
  void saveMessage_shouldSaveValidMessage() {
    // Given
    String validContent = "Hello World";
    MessageRequest request = new MessageRequest(validContent);

    // When
    messageService.saveMessage(request);

    // Then
    assertThat(messageService.getMessage()).isEqualTo(validContent);
  }

  @Test
  @DisplayName("saveMessage - 유효하지 않은 메시지 예외 발생")
  void saveMessage_shouldThrowException_whenContentIsInvalid() {
    // Given
    String invalidContent = "!@#$"; // 특수문자는 허용되지 않음
    MessageRequest request = new MessageRequest(invalidContent);

    // When & Then
    assertThatThrownBy(() -> messageService.saveMessage(request))
        .isInstanceOf(BadRequestException.class)
        .hasMessage(ErrorCode.MESSAGE_INVALID_REQUEST.getMessageKey());
  }

  @Test
  @DisplayName("getMessage - 저장된 메시지가 없을 경우 빈 문자열 반환")
  void getMessage_shouldReturnEmptyString_whenNoMessageStored() {
    // When
    String result = messageService.getMessage();

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("getMessage - 저장된 메시지 반환")
  void getMessage_shouldReturnStoredMessage() {
    // Given
    String content = "Test Message";
    messageService.saveMessage(new MessageRequest(content));

    // When
    String result = messageService.getMessage();

    // Then
    assertThat(result).isEqualTo(content);
  }
}
