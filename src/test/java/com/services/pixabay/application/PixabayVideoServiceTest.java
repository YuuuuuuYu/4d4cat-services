package com.services.pixabay.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.services.common.application.exception.BadGatewayException;
import com.services.common.application.exception.ErrorCode;
import com.services.common.application.exception.NotFoundException;
import com.services.common.infrastructure.ApiMetadata;
import com.services.common.infrastructure.DataStorage;
import com.services.pixabay.application.dto.request.PixabayVideoRequest;
import com.services.pixabay.application.dto.result.PixabayVideoResult;
import com.services.pixabay.fixture.PixabayTestFixtures;
import com.services.pixabay.presentation.dto.PixabayResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class PixabayVideoServiceTest {

  @Mock private RestTemplate restTemplate;
  @Mock private Environment environment;

  @InjectMocks private PixabayVideoService pixabayVideoService;

  private final String TEST_VIDEO_URL = "https://test.video.api/search";
  private final int CATEGORY_COUNT = 20;

  @Test
  @DisplayName("getBaseUrlKey - 기본 URL 프로퍼티 키 반환")
  void getBaseUrlKey_shouldReturnUrlPropertyKey() {
    // When
    String baseUrlKey = pixabayVideoService.getBaseUrlKey();

    // Then
    assertThat(baseUrlKey).isEqualTo(ApiMetadata.PIXABAY_VIDEOS.getUrlPropertyKey());
  }

  @Test
  @DisplayName("getStorageKey - 스토리지 키 반환")
  void getStorageKey_shouldReturnStorageKey() {
    // When
    String storageKey = pixabayVideoService.getStorageKey();

    // Then
    assertThat(storageKey).isEqualTo(ApiMetadata.PIXABAY_VIDEOS.getKey());
  }

  @Test
  @DisplayName("getFilters - 비디오 카테고리 목록 반환")
  void getFilters_shouldReturnVideoCategories() {
    // When
    List<String> filters = pixabayVideoService.getFilters();

    // Then
    assertThat(filters)
        .hasSize(CATEGORY_COUNT)
        .contains("backgrounds", "fashion", "nature", "science", "education");
  }

  @Test
  @DisplayName("createParameters - 카테고리를 포함한 PixabayVideoRequest 생성")
  void createParameters_shouldCreateRequestWithCategory() {
    PixabayVideoRequest request = pixabayVideoService.createParameters("backgrounds");

    assertAll(
        () -> assertThat(request).isNotNull(),
        () -> assertThat(request.getCategory()).isEqualTo("backgrounds"));
  }

  @Nested
  @DisplayName("setDataStorage - ")
  class Describe_setDataStorage {

    @BeforeEach
    void setUp() {
      when(environment.getProperty(ApiMetadata.PIXABAY_VIDEOS.getUrlPropertyKey()))
          .thenReturn(TEST_VIDEO_URL);

      PixabayResponse mockResponse =
          PixabayTestFixtures.createVideoResponse(
              List.of(PixabayTestFixtures.createDefaultVideoResult(1)));
      when(restTemplate.exchange(
              anyString(),
              eq(HttpMethod.GET),
              any(HttpEntity.class),
              any(ParameterizedTypeReference.class)))
          .thenReturn(ResponseEntity.ok(mockResponse));
    }

    @Test
    @DisplayName("데이터 초기화 시 카테고리 수만큼 API를 호출한다")
    void shouldCallApiPerCategory() {
      // When
      pixabayVideoService.setDataStorage();

      // Then
      verify(restTemplate, times(CATEGORY_COUNT))
          .exchange(
              anyString(),
              eq(HttpMethod.GET),
              any(HttpEntity.class),
              any(ParameterizedTypeReference.class));
    }

    @Test
    @DisplayName("API 호출 시 올바른 URI를 생성한다")
    void shouldBuildCorrectUriWhenCallingApi() {
      // When
      pixabayVideoService.setDataStorage();

      // Then
      ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
      verify(restTemplate, atLeastOnce())
          .exchange(
              urlCaptor.capture(),
              eq(HttpMethod.GET),
              any(HttpEntity.class),
              any(ParameterizedTypeReference.class));

      assertThat(urlCaptor.getAllValues()).allMatch(url -> url.startsWith(TEST_VIDEO_URL));
    }

    @Test
    @DisplayName("데이터 fetch가 완료되면 스토리지에 데이터를 저장한다")
    void shouldStoreDataWhenFetchCompletes() {
      try (MockedStatic<DataStorage> mockedDataStorage = mockStatic(DataStorage.class)) {
        // When
        pixabayVideoService.setDataStorage();

        // Then
        ArgumentCaptor<List<PixabayVideoResult>> dataCaptor = ArgumentCaptor.forClass(List.class);
        mockedDataStorage.verify(
            () ->
                DataStorage.setData(eq(ApiMetadata.PIXABAY_VIDEOS.getKey()), dataCaptor.capture()));

        assertThat(dataCaptor.getValue()).hasSize(CATEGORY_COUNT);
      }
    }

    @Test
    @DisplayName("API 호출 실패 시에도 중단 없이 계속 진행한다")
    void shouldContinueWhenApiCallFails() {
      // Given
      when(restTemplate.exchange(
              contains("fashion"),
              eq(HttpMethod.GET),
              any(HttpEntity.class),
              any(ParameterizedTypeReference.class)))
          .thenThrow(new NotFoundException(ErrorCode.DATA_NOT_FOUND));

      when(restTemplate.exchange(
              contains("nature"),
              eq(HttpMethod.GET),
              any(HttpEntity.class),
              any(ParameterizedTypeReference.class)))
          .thenThrow(new BadGatewayException(ErrorCode.BAD_GATEWAY));

      // When
      pixabayVideoService.setDataStorage();

      // Then
      verify(restTemplate, atLeast(3))
          .exchange(anyString(), eq(HttpMethod.GET), any(), any(ParameterizedTypeReference.class));
    }
  }

  @Test
  @DisplayName("getRandomElement - 데이터가 있을 때 비디오 데이터 반환")
  void getRandomElement_shouldReturnVideoDataWhenAvailable() {
    // Given
    PixabayVideoResult expectedVideo = PixabayTestFixtures.createDefaultVideoResult(1);

    try (MockedStatic<DataStorage> mockedDataStorage = mockStatic(DataStorage.class)) {
      mockedDataStorage
          .when(
              () ->
                  DataStorage.getRandomElement(
                      ApiMetadata.PIXABAY_VIDEOS.getKey(),
                      PixabayVideoResult.class,
                      ErrorCode.PIXABAY_VIDEO_NOT_FOUND))
          .thenReturn(expectedVideo);

      // When
      PixabayVideoResult actualVideo = pixabayVideoService.getRandomElement();

      // Then
      assertThat(actualVideo).isEqualTo(expectedVideo);
    }
  }

  @Test
  @DisplayName("getRandomElement - 데이터가 없을 때 NotFoundException 발생")
  void getRandomElement_shouldThrowNotFoundExceptionWhenDataIsEmpty() {
    // Given
    try (MockedStatic<DataStorage> mockedDataStorage = mockStatic(DataStorage.class)) {
      NotFoundException expectedException =
          new NotFoundException(ErrorCode.PIXABAY_VIDEO_NOT_FOUND);
      mockedDataStorage
          .when(
              () ->
                  DataStorage.getRandomElement(
                      ApiMetadata.PIXABAY_VIDEOS.getKey(),
                      PixabayVideoResult.class,
                      ErrorCode.PIXABAY_VIDEO_NOT_FOUND))
          .thenThrow(expectedException);

      // When & Then
      assertThatThrownBy(() -> pixabayVideoService.getRandomElement())
          .isInstanceOf(NotFoundException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.PIXABAY_VIDEO_NOT_FOUND);
    }
  }
}
