package com.services.api.pixabay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.services.core.infrastructure.RedisDataStorage;
import com.services.core.pixabay.dto.PixabayMusicResult;
import com.services.core.pixabay.dto.PixabayVideoResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PixabayServiceTest {

  @Mock private RedisDataStorage redisDataStorage;

  @InjectMocks private PixabayService pixabayService;

  @Test
  @DisplayName("getRandomVideo - 성공 시 비디오 결과 반환")
  void getRandomVideo_shouldReturnVideo() {
    // Given
    PixabayVideoResult videoResult = PixabayVideoResult.builder().id(1).build();
    when(redisDataStorage.getRandomElement(any(), eq(PixabayVideoResult.class), any()))
        .thenReturn(videoResult);

    // When
    PixabayVideoResult result = pixabayService.getRandomVideo();

    // Then
    assertThat(result).isEqualTo(videoResult);
  }

  @Test
  @DisplayName("getRandomMusic - 성공 시 음악 결과 반환")
  void getRandomMusic_shouldReturnMusic() {
    // Given
    PixabayMusicResult musicResult = PixabayMusicResult.builder().id(2).build();
    when(redisDataStorage.getRandomElement(any(), eq(PixabayMusicResult.class), any()))
        .thenReturn(musicResult);

    // When
    PixabayMusicResult result = pixabayService.getRandomMusic();

    // Then
    assertThat(result).isEqualTo(musicResult);
  }
}
