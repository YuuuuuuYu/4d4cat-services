package com.services.common.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.services.pixabay.application.dto.result.PixabayMusicResult;
import com.services.pixabay.application.dto.result.PixabayVideoResult;
import com.services.pixabay.fixture.PixabayTestFixtures;

class DataStorageTest {

    @BeforeEach
    void setUp() {
        DataStorage.clear();
    }

    @AfterEach
    void tearDown() {
        DataStorage.clear();
    }

    @Test
    @DisplayName("setData, getListData - 데이터 스토리지 저장 및 조회")
    void setDataAndGetListData_shouldReturnStorageData() {
        // Given
        String videoKey = "video-list";
        String musicKey = "music-list";
        int videoCount = 3;
        int musicCount = 4;
        
        PixabayTestFixtures.setDataStorageToReturnAllResult(videoCount,musicCount);

        // When
        Optional<List<PixabayVideoResult>> videoResult = DataStorage.getListData(videoKey, PixabayVideoResult.class);
        Optional<List<PixabayMusicResult>> musicResult = DataStorage.getListData(musicKey, PixabayMusicResult.class);

        // Then
        assertAll(
            () -> assertThat(videoResult).isPresent(),
            () -> assertThat(videoResult.get()).hasSize(videoCount),

            () -> assertThat(musicResult).isPresent(),
            () -> assertThat(musicResult.get()).hasSize(musicCount)
        );
    }

    @Test
    @DisplayName("setData, getListData - 저장하지 않은 데이터에 대해 조회하면 빈 리스트 반환")
    void setDataAndGetListData_shouldReturnEmptyElement_whenNotStoredData() {
        // Given
        String videoKey = "video-list";
        String musicKey = "music-list";
        int videoCount = 0;
        int musicCount = 0;

        PixabayTestFixtures.setDataStorageToReturnAllResult(videoCount,musicCount);
        
        // When
        Optional<List<PixabayVideoResult>> videoResult = DataStorage.getListData(videoKey, PixabayVideoResult.class);
        Optional<List<PixabayMusicResult>> musicResult = DataStorage.getListData(musicKey, PixabayMusicResult.class);
        
        // Then
        assertAll(
            () -> assertThat(videoResult.get()).isEmpty(),
            () -> assertThat(musicResult.get()).isEmpty()
        );
    }

    @Test
    @DisplayName("getRandomElement - 랜덤 요소 반환")
    void getRandomElement_shouldReturnRandomElement() {
        // Given
        String videoKey = "video-list";
        String musicKey = "music-list";
        int videoCount = 3;
        int musicCount = 4;
        
        PixabayTestFixtures.setDataStorageToReturnAllResult(videoCount,musicCount);

        // When
        Optional<PixabayVideoResult> videoResult = DataStorage.getRandomElement(videoKey, PixabayVideoResult.class);
        Optional<PixabayMusicResult> musicResult = DataStorage.getRandomElement(musicKey, PixabayMusicResult.class);

        // Then
        assertAll(
            () -> assertThat(videoResult).isPresent(),
            () -> assertThat(videoResult.get()).isInstanceOf(PixabayVideoResult.class),
            () -> assertThat(musicResult).isPresent(),
            () -> assertThat(musicResult.get()).isInstanceOf(PixabayMusicResult.class)
        );
    }

    @Test
    @DisplayName("getRandomElement - 저장하지 않은 데이터에 대해 조회하면 빈 Optional 반환")
    void getRandomElement_shouldReturnEmptyElement_whenNotStoredData() {
        // Given
        String videoKey = "video-list";
        String musicKey = "music-list";
        int videoCount = 0;
        int musicCount = 0;

        PixabayTestFixtures.setDataStorageToReturnAllResult(videoCount,musicCount);

        // When
        Optional<PixabayVideoResult> videoResult = DataStorage.getRandomElement(videoKey, PixabayVideoResult.class);
        Optional<PixabayMusicResult> musicResult = DataStorage.getRandomElement(musicKey, PixabayMusicResult.class);

        // Then
        assertAll(
            () -> assertThat(videoResult).isEmpty(),
            () -> assertThat(musicResult).isEmpty()
        );
    }

    @Test
    @DisplayName("getRandomElement - 존재하지 않는 키에 대해 조회하면 빈 Optional 반환")
    void getRandomElement_shouldReturnEmptyElement_whenGetNotExistKey() {
        // Given
        String nonExistentKey = "non-existent";
        int videoCount = 1;
        int musicCount = 1;
        
        PixabayTestFixtures.setDataStorageToReturnAllResult(videoCount,musicCount);

        // When
        Optional<PixabayVideoResult> videoResult = DataStorage.getRandomElement(nonExistentKey, PixabayVideoResult.class);
        Optional<PixabayMusicResult> musicResult = DataStorage.getRandomElement(nonExistentKey, PixabayMusicResult.class);

        // Then
        assertAll(
            () -> assertThat(videoResult).isEmpty(),
            () -> assertThat(musicResult).isEmpty()
        );
    }

    @Test
    @DisplayName("clear - 데이터 스토리지 초기화")
    void clear_shouldClearAllStoredData() {
        // Given
        DataStorage.setData("key1", List.of("item1"));
        DataStorage.setData("key2", List.of("item2"));
        DataStorage.setData("key3", List.of("item1", "item2"));

        // When
        DataStorage.clear();

        // Then
        assertAll(
            () -> assertThat(DataStorage.getListData("key1", String.class)).isEmpty(),
            () -> assertThat(DataStorage.getListData("key2", String.class)).isEmpty(),
            () -> assertThat(DataStorage.getListData("key3", String.class)).isEmpty()
        );
    }
}
