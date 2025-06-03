package com.services.pixabay.application;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertAll;

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
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.Model;
import org.springframework.web.client.RestTemplate;

import com.services.common.exception.BadRequestException;
import com.services.common.exception.ErrorCode;
import com.services.common.infrastructure.ApiMetadata;
import com.services.common.infrastructure.DataStorage;
import com.services.pixabay.application.dto.request.PixabayVideoRequest;
import com.services.pixabay.application.dto.result.PixabayVideoResult;
import com.services.pixabay.presentation.dto.PixabayResponse;

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
    @DisplayName("getBaseUrl Success")
    void getBaseUrl_returnsPixabayVideoUrl() {
        String baseUrl = pixabayVideoService.getBaseUrl();
        
        assertThat(baseUrl).isEqualTo(ApiMetadata.PIXABAY_VIDEOS.getUrl());
    }

    @Test
    @DisplayName("getStorageKey Success")
    void getStorageKey_returnsPixabayVideoKey() {
        String storageKey = pixabayVideoService.getStorageKey();
        
        assertThat(storageKey).isEqualTo(ApiMetadata.PIXABAY_VIDEOS.getKey());
    }

    @Test
    @DisplayName("getFilters Success")
    void getFilters_returnsVideoCategories() {
        List<String> filters = pixabayVideoService.getFilters();
        
        assertAll(
            () -> assertThat(filters).contains("backgrounds", "fashion", "nature", "science", "education"),
            () -> assertThat(filters).hasSize(20)
        );
    }

    @Test
    @DisplayName("createParameters Success")
    void createParameters_createsPixabayVideoRequest() {
        PixabayVideoRequest request = pixabayVideoService.createParameters("backgrounds");
        
        assertAll(
            () -> assertThat(request).isNotNull(),
            () -> assertThat(request.getKey()).isEqualTo("test-api-key"),
            () -> assertThat(request.getCategory()).isEqualTo("backgrounds")
        );
    }

    @Test
    @DisplayName("setDataStorage Success - 20회 호출, 빌더 URI 체크, 데이터 저장 확인")
    void setDataStorage_setDataStorage() {
        // Given
        String total = "1";
        String totalHits = "1";
        String videos = "{\"small\":\"https://cdn.pixabay.com/video/2015/08/08/125-135736646_small.mp4\",\"medium\":\"https://cdn.pixabay.com/video/2015/08/08/125-135736646_medium.mp4\",\"large\":\"https://cdn.pixabay.com/video/2015/08/08/125-135736646_large.mp4\"}";
        PixabayVideoResult videoResult = PixabayVideoResult.builder()
            .id(1)
            .pageURL("https://pixabay.com/videos/id-125/")
            .type("video")
            .tags("nature, video")
            .duration(120)
            .videos(videos)
            .views(1000)
            .downloads(100)
            .likes(50)
            .comments(10)
            .user_id(123)
            .user("Coverr-Free-Footage")
            .userImageURL("https://cdn.pixabay.com/user/2015/10/16/09-28-45-303_250x250.png")
            .noAiTraining(false)
            .build();

        PixabayResponse<PixabayVideoResult> response = new PixabayResponse<>(total, totalHits, List.of(videoResult));

        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            isNull(),
            any(ParameterizedTypeReference.class)
        )).thenReturn(ResponseEntity.ok(response));

        // When
        pixabayVideoService.setDataStorage();

        // Then
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        List<String> capturedUrls = urlCaptor.getAllValues();
        Optional<List<PixabayVideoResult>> storedData = DataStorage.getListData(
            ApiMetadata.PIXABAY_VIDEOS.getKey(), 
            PixabayVideoResult.class
        );

        verify(restTemplate, times(20)).exchange(
            urlCaptor.capture(),
            eq(HttpMethod.GET),
            isNull(),
            any(ParameterizedTypeReference.class)
        );
        
        assertAll(
            () -> assertThat(capturedUrls).allMatch(
                url -> url.contains(ApiMetadata.PIXABAY_VIDEOS.getUrl())),
            () -> assertThat(storedData).isPresent(),
            () -> assertThat(storedData.get()).hasSize(20),
            () -> assertThat(storedData.get().get(0)).isEqualTo(videoResult)
        );
    }

    @Test
    @DisplayName("setDataStorage Fail - BadRequestException")
    void setDataStorage_throwsBadRequestException_onInvalidResponse() {
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
    @DisplayName("addRandomElementToModel Success - Model에 비디오 추가")
    void addRandomElementToModel_addsVideoToModel() {
        // Given
        String videos = "{\"small\":\"https://cdn.pixabay.com/video/2015/08/08/125-135736646_small.mp4\",\"medium\":\"https://cdn.pixabay.com/video/2015/08/08/125-135736646_medium.mp4\",\"large\":\"https://cdn.pixabay.com/video/2015/08/08/125-135736646_large.mp4\"}";
        PixabayVideoResult videoResult = PixabayVideoResult.builder()
            .id(1)
            .pageURL("https://pixabay.com/videos/id-125/")
            .type("video")
            .tags("nature, video")
            .duration(120)
            .videos(videos)
            .views(1000)
            .downloads(100)
            .likes(50)
            .comments(10)
            .user_id(123)
            .user("Coverr-Free-Footage")
            .userImageURL("https://cdn.pixabay.com/user/2015/10/16/09-28-45-303_250x250.png")
            .noAiTraining(false)
            .build();

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
    @DisplayName("addRandomElementToModel Fail - 데이터가 없을 경우 모델에 추가하지 않음")
    void addRandomElementToModel_noDataAvailable_doesNotAddToModel() {
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