package com.services.pixabay.presentation;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.services.common.application.exception.ErrorCode;
import com.services.common.application.exception.NotFoundException;
import com.services.pixabay.application.PixabayMusicService;
import com.services.pixabay.application.PixabayVideoService;
import com.services.pixabay.application.dto.result.PixabayMusicResult;
import com.services.pixabay.application.dto.result.PixabayVideoResult;
import com.services.pixabay.fixture.PixabayTestFixtures;

@WebMvcTest(PixabayController.class)
class PixabayControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PixabayVideoService pixabayVideoService;

    @MockitoBean
    private PixabayMusicService pixabayMusicService;

    @Test
    @DisplayName("GET /video - 비디오 데이터 성공 응답")
    void getVideo_shouldReturnVideoData() throws Exception {
        // Given
        PixabayVideoResult videoResult = PixabayTestFixtures.createDefaultVideoResult(1);
        when(pixabayVideoService.getRandomElement()).thenReturn(videoResult);

        // When & Then
        mockMvc.perform(get("/video"))
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
        when(pixabayMusicService.getRandomElement()).thenReturn(musicResult);

        // When & Then
        mockMvc.perform(get("/music"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.download_url").value("https://pixabay.com/music/download/id-1111.mp3"))
            .andExpect(jsonPath("$.error").isEmpty());
    }

    @Test
    @DisplayName("GET /video - 데이터 없을 때 404 에러 응답")
    void getVideo_shouldReturn404_whenDataNotFound() throws Exception {
        // Given
        when(pixabayVideoService.getRandomElement()).thenThrow(new NotFoundException(ErrorCode.PIXABAY_VIDEO_NOT_FOUND));

        // When & Then
        mockMvc.perform(get("/video"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.data").isEmpty())
            .andExpect(jsonPath("$.error.code").value("PV1000"))
            .andExpect(jsonPath("$.error.message").value("PixabayVideo data not found"));
    }

    @Test
    @DisplayName("GET /music - 데이터 없을 때 404 에러 응답")
    void getMusic_shouldReturn404_whenDataNotFound() throws Exception {
        // Given
        when(pixabayMusicService.getRandomElement()).thenThrow(new NotFoundException(ErrorCode.PIXABAY_MUSIC_NOT_FOUND));

        // When & Then
        mockMvc.perform(get("/music"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.data").isEmpty())
            .andExpect(jsonPath("$.error.code").value("PM1000"))
            .andExpect(jsonPath("$.error.message").value("PixabayMusic data not found"));
    }
}
