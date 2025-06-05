package com.services.pixabay.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.Model;

import com.services.pixabay.application.PixabayMusicService;
import com.services.pixabay.application.PixabayVideoService;

@WebMvcTest(PixabayController.class)
class PixabayControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PixabayVideoService pixabayVideoService;

    @MockitoBean
    private PixabayMusicService pixabayMusicService;

    @Test
    @DisplayName("GET /video - 비디오 페이지 반환")
    void getVideo_shouldReturnVideoPage() throws Exception {
        // When & Then
        mockMvc.perform(get("/video"))
            .andExpect(status().isOk())
            .andExpect(view().name("video"));
        
        verify(pixabayVideoService).addRandomElementToModel(any(Model.class));
    }

    @Test
    @DisplayName("GET /music - 음악 페이지 반환")
    void getMusic_shouldReturnMusicPage() throws Exception {
        // When & Then
        mockMvc.perform(get("/music"))
            .andExpect(status().isOk())
            .andExpect(view().name("music"));
        
        verify(pixabayMusicService).addRandomElementToModel(any(Model.class));
    }
}
