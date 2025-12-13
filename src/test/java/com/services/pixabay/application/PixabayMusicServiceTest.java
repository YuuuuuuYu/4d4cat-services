package com.services.pixabay.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

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
import org.springframework.web.client.RestTemplate;

import com.services.common.application.exception.BadGatewayException;
import com.services.common.application.exception.ErrorCode;
import com.services.common.application.exception.NotFoundException;
import com.services.common.infrastructure.ApiMetadata;
import com.services.common.infrastructure.DataStorage;
import com.services.pixabay.application.dto.request.PixabayMusicRequest;
import com.services.pixabay.application.dto.result.PixabayMusicResult;
import com.services.pixabay.fixture.PixabayTestFixtures;
import com.services.pixabay.presentation.dto.CustomPixabayMusicResponse;

@ExtendWith(MockitoExtension.class)
class PixabayMusicServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private PixabayMusicService pixabayMusicService;

    @Test
    @DisplayName("getBaseUrl - 기본 URL 반환")
    void getBaseUrl_shouldReturnsPixabayMusicUrl() {
        String baseUrl = pixabayMusicService.getBaseUrl();
        
        assertThat(baseUrl).isEqualTo(ApiMetadata.PIXABAY_MUSIC.getUrl());
    }

    @Test
    @DisplayName("getFilters - 음악 장르 목록 반환")
    void getFilters_shouldReturnsMusicGenres() {
        int currentGenreSize = 32;
        List<String> filters = pixabayMusicService.getFilters();
        
        assertAll(
            () -> assertThat(filters).contains("electronic"),
            () -> assertThat(filters).hasSize(currentGenreSize)
        );
        
    }

    @Test
    @DisplayName("createParameters - PixabayMusicRequest 생성")
    void createParameters_shouldCreatesPixabayMusicRequest() {
        PixabayMusicRequest request = pixabayMusicService.createParameters("electronic");
        
        assertAll(
            () -> assertThat(request).isNotNull(),
            () -> assertThat(request.getGenre()).isEqualTo("electronic")
        );
    }

    @Test
    @DisplayName("setDataStorage - 데이터 저장할 때 API는 장르 수만큼 호출")
    void setDataStorage_shouldCallApiManyTimes_whenInitializingData() {
        // Given
        int callApiCount = 32;
        PixabayTestFixtures.setupRestTemplateToReturnSingleMusic(restTemplate, 32);

        // When
        pixabayMusicService.setDataStorage();

        // Then
        verify(restTemplate, times(callApiCount)).exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class)
        );
    }

    @Test
    @DisplayName("setDataStorage - API 호출 시 올바른 URI가 생성되어야 함")
    void setDataStorage_shouldBuildCorrectUri_whenCallingApi() {
        // Given
        PixabayTestFixtures.setupRestTemplateToReturnSingleMusic(restTemplate, 32);

        // When
        pixabayMusicService.setDataStorage();

        // Then
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        List<String> capturedUrls = urlCaptor.getAllValues();

        verify(restTemplate, atLeastOnce()).exchange(
            urlCaptor.capture(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class)
        );
        
        assertThat(capturedUrls).allMatch(
            url -> url.contains(ApiMetadata.PIXABAY_MUSIC.getUrl()));
    }

    @Test
    @DisplayName("setDataStorage - 데이터 fetch가 완료되면 스토리지에 데이터 저장")
    void setDataStorage_shouldStoreDataInStorage_whenDataFetchCompletes() {
        // Given
        CustomPixabayMusicResponse mockResponse = PixabayTestFixtures.createMusicResponse(
            List.of(PixabayTestFixtures.createDefaultMusicResult(1))
        );
        
        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class)
        )).thenReturn(ResponseEntity.ok(mockResponse));

        // When
        pixabayMusicService.setDataStorage();

        Optional<List<PixabayMusicResult>> storedData = DataStorage.getListData(
            ApiMetadata.PIXABAY_MUSIC.getKey(), 
            PixabayMusicResult.class
        );

        // Then
        verify(restTemplate, atLeastOnce()).exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class)
        );
        
        assertAll(
            () -> assertThat(storedData).isPresent(),
            () -> assertThat(storedData.get()).hasSize(32) // 32개 장르 각각에서 1개씩 총 32개
        );
    }

    @Test
    @DisplayName("setDataStorage - NotFoundException(404) 발생시 해당 필터만 제외하고 계속 진행")
    void setDataStorage_shouldContinueWithOtherFilters_whenNotFoundOccurs() {
        // Given
        CustomPixabayMusicResponse successResponse = PixabayTestFixtures.createMusicResponse(
            List.of(PixabayTestFixtures.createDefaultMusicResult(1))
        );

        when(restTemplate.exchange(
            contains("electronic"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class)
        )).thenReturn(ResponseEntity.ok(successResponse));
        
        when(restTemplate.exchange(
            contains("upbeat"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class)
        )).thenThrow(new NotFoundException(ErrorCode.DATA_NOT_FOUND));

        // When
        pixabayMusicService.setDataStorage();

        // Then - exceptionally()로 인해 예외가 발생하지 않고 성공한 데이터만으로 초기화됨
        verify(restTemplate, atLeast(2)).exchange(anyString(), eq(HttpMethod.GET), any(), any(ParameterizedTypeReference.class));
    }

    @Test
    @DisplayName("setDataStorage - BadGatewayException(502) 발생시 해당 필터만 제외하고 계속 진행")
    void setDataStorage_shouldContinueWithOtherFilters_whenBadGatewayOccurs() {
        // Given
        CustomPixabayMusicResponse successResponse = PixabayTestFixtures.createMusicResponse(
            List.of(PixabayTestFixtures.createDefaultMusicResult(1))
        );

        when(restTemplate.exchange(
            contains("electronic"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class)
        )).thenReturn(ResponseEntity.ok(successResponse));
        
        when(restTemplate.exchange(
            contains("upbeat"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class)
        )).thenThrow(new BadGatewayException(ErrorCode.BAD_GATEWAY));

        // When
        pixabayMusicService.setDataStorage();

        // Then - exceptionally()로 인해 예외가 발생하지 않고 성공한 데이터만으로 초기화됨
        verify(restTemplate, atLeast(2)).exchange(anyString(), eq(HttpMethod.GET), any(), any(ParameterizedTypeReference.class));
    }

    @Test
    @DisplayName("getRandomElement - 데이터가 있을 때 음악 데이터 반환")
    void getRandomElement_shouldReturnsMusicData_whenDataAvailable() {
        // Given
        PixabayMusicResult musicResult = PixabayTestFixtures.createDefaultMusicResult(1);

        try (MockedStatic<DataStorage> mockedDataStorage = mockStatic(DataStorage.class)) {
            mockedDataStorage.when(() -> DataStorage.getRandomElement(
                ApiMetadata.PIXABAY_MUSIC.getKey(),
                PixabayMusicResult.class,
                ErrorCode.PIXABAY_MUSIC_NOT_FOUND
            )).thenReturn(musicResult);
            
            // When
            PixabayMusicResult result = pixabayMusicService.getRandomElement();

            // Then
            assertThat(result).isEqualTo(musicResult);
        }
    }

    @Test
    @DisplayName("getRandomElement - 데이터가 없을 때 NotFoundException 발생")
    void getRandomElement_shouldThrowNotFoundException_whenDataNotFound() {
        // Given
        try (MockedStatic<DataStorage> mockedDataStorage = mockStatic(DataStorage.class)) {
            mockedDataStorage.when(() -> DataStorage.getRandomElement(
                ApiMetadata.PIXABAY_MUSIC.getKey(),
                PixabayMusicResult.class,
                ErrorCode.PIXABAY_MUSIC_NOT_FOUND
            )).thenThrow(new NotFoundException(ErrorCode.PIXABAY_MUSIC_NOT_FOUND));
            
            // When & Then
            assertThatThrownBy(() -> pixabayMusicService.getRandomElement())
                .isInstanceOf(NotFoundException.class)
                .hasMessage(ErrorCode.PIXABAY_MUSIC_NOT_FOUND.getMessageKey());
        }
    }
}