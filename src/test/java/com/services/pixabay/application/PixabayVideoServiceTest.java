package com.services.pixabay.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class PixabayVideoServiceTest {

  @Mock private RestTemplate restTemplate;

  @InjectMocks private PixabayVideoService pixabayVideoService;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(pixabayVideoService, "key", "test-api-key");
  }

  @Test
  @DisplayName("getBaseUrl - 기본 URL 반환")
  void getBaseUrl_shouldReturnsPixabayVideoUrl() {
    String baseUrl = pixabayVideoService.getBaseUrl();

    assertThat(baseUrl).isEqualTo(ApiMetadata.PIXABAY_VIDEOS.getUrl());
  }

  @Test
  @DisplayName("getStorageKey - 스토리지 키 반환")
  void getStorageKey_shouldReturnsPixabayVideoKey() {
    String storageKey = pixabayVideoService.getStorageKey();

    assertThat(storageKey).isEqualTo(ApiMetadata.PIXABAY_VIDEOS.getKey());
  }

  @Test
  @DisplayName("getFilters - 비디오 카테고리 목록 반환")
  void getFilters_shouldReturnsVideoCategories() {
    int currentCategorySize = 20;
    List<String> filters = pixabayVideoService.getFilters();

    assertAll(
        () ->
            assertThat(filters)
                .contains("backgrounds", "fashion", "nature", "science", "education"),
        () -> assertThat(filters).hasSize(currentCategorySize));
  }

  @Test
  @DisplayName("createParameters - PixabayVideoRequest 생성")
  void createParameters_shouldCreatesPixabayVideoRequest() {
    PixabayVideoRequest request = pixabayVideoService.createParameters("backgrounds");

    assertAll(
        () -> assertThat(request).isNotNull(),
        () -> assertThat(request.getKey()).isEqualTo("test-api-key"),
        () -> assertThat(request.getCategory()).isEqualTo("backgrounds"));
  }

  @Test
  @DisplayName("setDataStorage - 데이터 저장할 때 API는 20회 호출")
  void setDataStorage_shouldCallApiManyTimes_whenInitializingData() {
    // Given
    int callApiCount = 20;
    PixabayTestFixtures.setupRestTemplateToReturnSingleVideo(restTemplate, 1);

    // When
    pixabayVideoService.setDataStorage();

    // Then
    verify(restTemplate, times(callApiCount))
        .exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class));
  }

  @Test
  @DisplayName("setDataStorage - API 호출 시 올바른 URI가 생성되어야 함")
  void setDataStorage_shouldBuildCorrectUri_whenCallingApi() {
    // Given
    PixabayTestFixtures.setupRestTemplateToReturnSingleVideo(restTemplate, 1);

    // When
    pixabayVideoService.setDataStorage();

    // Then
    ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
    List<String> capturedUrls = urlCaptor.getAllValues();

    verify(restTemplate, atLeastOnce())
        .exchange(
            urlCaptor.capture(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class));

    assertThat(capturedUrls).allMatch(url -> url.contains(ApiMetadata.PIXABAY_VIDEOS.getUrl()));
  }

  @Test
  @DisplayName("setDataStorage - 데이터 fetch가 완료되면 스토리지에 데이터 저장")
  void setDataStorage_shouldStoreDataInStorage_whenDataFetchCompletes() {
    // Given
    PixabayResponse mockResponse =
        PixabayTestFixtures.createVideoResponse(
            List.of(PixabayTestFixtures.createDefaultVideoResult(1)));

    when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class)))
        .thenReturn(ResponseEntity.ok(mockResponse));

    // When
    pixabayVideoService.setDataStorage();

    Optional<List<PixabayVideoResult>> storedData =
        DataStorage.getListData(ApiMetadata.PIXABAY_VIDEOS.getKey(), PixabayVideoResult.class);

    // Then
    verify(restTemplate, atLeastOnce())
        .exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class));

    assertAll(
        () -> assertThat(storedData).isPresent(),
        () -> assertThat(storedData.get()).hasSize(20) // 20개 카테고리 각각에서 1개씩 총 20개
        );
  }

  @Test
  @DisplayName("setDataStorage - NotFoundException(404) 발생시 해당 필터만 제외하고 계속 진행")
  void setDataStorage_shouldContinueWithOtherFilters_whenNotFoundOccurs() {
    // Given
    PixabayResponse successResponse =
        PixabayTestFixtures.createVideoResponse(
            List.of(PixabayTestFixtures.createDefaultVideoResult(1)));

    when(restTemplate.exchange(
            contains("backgrounds"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class)))
        .thenReturn(ResponseEntity.ok(successResponse));

    when(restTemplate.exchange(
            contains("fashion"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class)))
        .thenThrow(new NotFoundException(ErrorCode.DATA_NOT_FOUND));

    // When
    pixabayVideoService.setDataStorage();

    // Then - exceptionally()로 인해 예외가 발생하지 않고 성공한 데이터만으로 초기화됨
    verify(restTemplate, atLeast(2))
        .exchange(anyString(), eq(HttpMethod.GET), any(), any(ParameterizedTypeReference.class));
  }

  @Test
  @DisplayName("setDataStorage - BadGatewayException(502) 발생시 해당 필터만 제외하고 계속 진행")
  void setDataStorage_shouldContinueWithOtherFilters_whenBadGatewayOccurs() {
    // Given
    PixabayResponse successResponse =
        PixabayTestFixtures.createVideoResponse(
            List.of(PixabayTestFixtures.createDefaultVideoResult(1)));

    when(restTemplate.exchange(
            contains("backgrounds"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class)))
        .thenReturn(ResponseEntity.ok(successResponse));

    when(restTemplate.exchange(
            contains("fashion"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class)))
        .thenThrow(new BadGatewayException(ErrorCode.BAD_GATEWAY));

    // When
    pixabayVideoService.setDataStorage();

    // Then - exceptionally()로 인해 예외가 발생하지 않고 성공한 데이터만으로 초기화됨
    verify(restTemplate, atLeast(2))
        .exchange(anyString(), eq(HttpMethod.GET), any(), any(ParameterizedTypeReference.class));
  }

  @Test
  @DisplayName("getRandomElement - 데이터가 있을 때 비디오 데이터 반환")
  void getRandomElement_shouldReturnsVideoData_whenDataAvailable() {
    // Given
    PixabayVideoResult videoResult = PixabayTestFixtures.createDefaultVideoResult(1);

    try (MockedStatic<DataStorage> mockedDataStorage = mockStatic(DataStorage.class)) {
      mockedDataStorage
          .when(
              () ->
                  DataStorage.getRandomElement(
                      ApiMetadata.PIXABAY_VIDEOS.getKey(),
                      PixabayVideoResult.class,
                      ErrorCode.PIXABAY_VIDEO_NOT_FOUND))
          .thenReturn(videoResult);

      // When
      PixabayVideoResult result = pixabayVideoService.getRandomElement();

      // Then
      assertThat(result).isEqualTo(videoResult);
    }
  }

  @Test
  @DisplayName("getRandomElement - 데이터가 없을 때 NotFoundException 발생")
  void getRandomElement_shouldThrowNotFoundException_whenDataNotFound() {
    // Given
    try (MockedStatic<DataStorage> mockedDataStorage = mockStatic(DataStorage.class)) {
      mockedDataStorage
          .when(
              () ->
                  DataStorage.getRandomElement(
                      ApiMetadata.PIXABAY_VIDEOS.getKey(),
                      PixabayVideoResult.class,
                      ErrorCode.PIXABAY_VIDEO_NOT_FOUND))
          .thenThrow(new NotFoundException(ErrorCode.PIXABAY_VIDEO_NOT_FOUND));

      // When & Then
      assertThatThrownBy(() -> pixabayVideoService.getRandomElement())
          .isInstanceOf(NotFoundException.class)
          .hasMessage(ErrorCode.PIXABAY_VIDEO_NOT_FOUND.getMessageKey());
    }
  }
}
