package com.services.api.pixabay;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.services.api.config.MessageSourceConfig;
import com.services.api.fixture.PixabayTestFixtures;
import com.services.core.exception.ErrorCode;
import com.services.core.exception.NotFoundException;
import com.services.core.pixabay.dto.PixabayMusicResult;
import com.services.core.pixabay.dto.PixabayVideoResult;
import java.util.Locale;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PixabayController.class)
@Import(MessageSourceConfig.class)
@ActiveProfiles("test")
class PixabayControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private MessageSource messageSource;

  @MockitoBean private PixabayService pixabayService;

  private String getErrorMessage(ErrorCode errorCode) {
    return messageSource.getMessage(errorCode.getMessageKey(), null, Locale.getDefault());
  }

  @Test
  @DisplayName("GET /video - 비디오 데이터 성공 응답")
  void getVideo_shouldReturnVideoData() throws Exception {
    // Given
    PixabayVideoResult videoResult = PixabayTestFixtures.createDefaultVideoResult(1);
    when(pixabayService.getRandomVideo()).thenReturn(videoResult);

    // When & Then
    mockMvc
        .perform(get("/video"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200))
        .andExpect(jsonPath("$.data.id").value(1))
        .andExpect(jsonPath("$.data.pageURL").value("https://pixabay.com/videos/id-125/"))
        .andExpect(jsonPath("$.error").isEmpty());
  }

  @Test
  @DisplayName("GET /music - 음악 데이터 성공 응답")
  void getMusic_shouldReturnMusicData() throws Exception {
    // Given
    PixabayMusicResult musicResult = PixabayTestFixtures.createDefaultMusicResult(1);
    when(pixabayService.getRandomMusic()).thenReturn(musicResult);

    // When & Then
    mockMvc
        .perform(get("/music"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200))
        .andExpect(jsonPath("$.data.id").value(1))
        .andExpect(
            jsonPath("$.data.download_url").value("https://pixabay.com/music/download/id-1111.mp3"))
        .andExpect(jsonPath("$.error").isEmpty());
  }

  @Test
  @DisplayName("GET /video - 데이터 없을 때 404 에러 응답")
  void getVideo_shouldReturn404_whenDataNotFound() throws Exception {
    // Given
    ErrorCode errorCode = ErrorCode.PIXABAY_VIDEO_NOT_FOUND;
    when(pixabayService.getRandomVideo()).thenThrow(new NotFoundException(errorCode));
    String expectedMessage = getErrorMessage(errorCode);

    // When & Then
    mockMvc
        .perform(get("/video"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.data").isEmpty())
        .andExpect(jsonPath("$.error.code").value(errorCode.getCode()))
        .andExpect(jsonPath("$.error.message").value(expectedMessage));
  }

  @Test
  @DisplayName("GET /music - 데이터 없을 때 404 에러 응답")
  void getMusic_shouldReturn404_whenDataNotFound() throws Exception {
    // Given
    ErrorCode errorCode = ErrorCode.PIXABAY_MUSIC_NOT_FOUND;
    when(pixabayService.getRandomMusic()).thenThrow(new NotFoundException(errorCode));
    String expectedMessage = getErrorMessage(errorCode);

    // When & Then
    mockMvc
        .perform(get("/music"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.data").isEmpty())
        .andExpect(jsonPath("$.error.code").value(errorCode.getCode()))
        .andExpect(jsonPath("$.error.message").value(expectedMessage));
  }
}
