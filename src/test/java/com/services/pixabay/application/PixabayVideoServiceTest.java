package com.services.pixabay.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.springframework.http.HttpMethod;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.Model;
import org.springframework.web.client.RestTemplate;

import com.services.common.exception.BadRequestException;
import com.services.common.exception.ErrorCode;
import com.services.common.infrastructure.ApiMetadata;
import com.services.common.infrastructure.DataStorage;
import com.services.pixabay.application.dto.request.PixabayVideoRequest;
import com.services.pixabay.application.dto.result.PixabayVideoResult;
import com.services.pixabay.fixture.PixabayTestFixtures;

@ExtendWith(MockitoExtension.class)
class PixabayVideoServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private Model model;

    @InjectMocks
    private PixabayVideoService pixabayVideoService;

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
            () -> assertThat(filters).contains("backgrounds", "fashion", "nature", "science", "education"),
            () -> assertThat(filters).hasSize(currentCategorySize)
        );
    }

    @Test
    @DisplayName("createParameters - PixabayVideoRequest 생성")
    void createParameters_shouldCreatesPixabayVideoRequest() {
        PixabayVideoRequest request = pixabayVideoService.createParameters("backgrounds");
        
        assertAll(
            () -> assertThat(request).isNotNull(),
            () -> assertThat(request.getKey()).isEqualTo("test-api-key"),
            () -> assertThat(request.getCategory()).isEqualTo("backgrounds")
        );
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
        verify(restTemplate, times(callApiCount)).exchange(
            anyString(),
            eq(HttpMethod.GET),
            isNull(),
            any(ParameterizedTypeReference.class)
        );
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

        verify(restTemplate, atLeastOnce()).exchange(
            urlCaptor.capture(),
            eq(HttpMethod.GET),
            isNull(),
            any(ParameterizedTypeReference.class)
        );
        
        assertThat(capturedUrls).allMatch(
            url -> url.contains(ApiMetadata.PIXABAY_VIDEOS.getUrl()));
    }

    @Test
    @DisplayName("setDataStorage - 데이터 fetch가 완료되면 스토리지에 데이터 저장")
    void setDataStorage_shouldStoreDataInStorage_whenDataFetchCompletes() {
        // Given
        PixabayTestFixtures.setupRestTemplateToReturnSingleVideo(restTemplate, 1);

        // When
        pixabayVideoService.setDataStorage();

        Optional<List<PixabayVideoResult>> storedData = DataStorage.getListData(
            ApiMetadata.PIXABAY_VIDEOS.getKey(), 
            PixabayVideoResult.class
        );

        // Then
        verify(restTemplate, atLeastOnce()).exchange(
            anyString(),
            eq(HttpMethod.GET),
            isNull(),
            any(ParameterizedTypeReference.class)
        );
        
        assertAll(
            () -> assertThat(storedData).isPresent(),
            () -> assertThat(storedData.get()).hasSize(20)
        );
    }

    @Test
    @DisplayName("setDataStorage - API 호출이 실패하면 BadRequestException을 던진다")
    void setDataStorage_shouldThrowBadRequestException_whenApiCallFails() {
        // Given
        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            isNull(),
            any(ParameterizedTypeReference.class)
        )).thenThrow(new BadRequestException(ErrorCode.INVALID_REQUEST));

        // When, Then
        assertThatThrownBy(() -> pixabayVideoService.setDataStorage())
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("Invalid request");
    }

    @Test
    @DisplayName("addRandomElementToModel - 데이터가 있을 때 모델에 비디오 추가")
    void addRandomElementToModel_shouldAddsVideoToModel_whenDataAvailable() {
        // Given
        PixabayVideoResult videoResult = PixabayTestFixtures.createDefaultVideoResult(1);

        // When, Then
        try (MockedStatic<DataStorage> mockedDataStorage = mockStatic(DataStorage.class)) {
            mockedDataStorage.when(() -> DataStorage.getRandomElement(
                ApiMetadata.PIXABAY_VIDEOS.getKey(),
                PixabayVideoResult.class
            )).thenReturn(Optional.of(videoResult));
            
            pixabayVideoService.addRandomElementToModel(model);

            verify(model).addAttribute(
                ApiMetadata.PIXABAY_VIDEOS.getAttributeName(),
                videoResult
            );
        }
    }

    @Test
    @DisplayName("addRandomElementToModel - 데이터가 없을 경우 모델에 추가하지 않음")
    void addRandomElementToModel_shouldNotAddsVideoToModel_whenDataNotFound() {
        // Given
        try (MockedStatic<DataStorage> mockedDataStorage = mockStatic(DataStorage.class)) {
            mockedDataStorage.when(() -> DataStorage.getRandomElement(
                ApiMetadata.PIXABAY_VIDEOS.getKey(),
                PixabayVideoResult.class
            )).thenReturn(Optional.empty());
            
            // When
            pixabayVideoService.addRandomElementToModel(model);

            // Then
            verify(model, never()).addAttribute(anyString(), any());
        }
    }
}