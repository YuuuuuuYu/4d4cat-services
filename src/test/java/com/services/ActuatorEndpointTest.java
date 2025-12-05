package com.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.services.message.application.MessageService;
import com.services.pixabay.application.PixabayMusicService;
import com.services.pixabay.application.PixabayVideoService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ActuatorEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MessageService messageService;
    @MockitoBean
    private PixabayVideoService pixabayVideoService;
    @MockitoBean
    private PixabayMusicService pixabayMusicService;

    @Test
    @DisplayName("GET /actuator/health - 200 OK와 status UP 응답")
    void healthEndpoint_shouldReturnOkAndUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("GET /actuator/info - 200 OK 응답")
    void infoEndpoint_shouldReturnOk() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /actuator/prometheus - 200 OK 응답")
    void prometheusEndpoint_shouldReturnOk() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /actuator/beans - 노출되지 않은 엔드포인트 404 Not Found 응답")
    void beansEndpoint_shouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/actuator/beans"))
                .andExpect(status().isNotFound());
    }
}