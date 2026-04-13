package com.services.api.techblog;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.services.api.common.config.MessageSourceConfig;
import com.services.api.techblog.dto.TechBlogCompanyResponse;
import com.services.api.techblog.dto.TechBlogListResponse;
import com.services.api.techblog.dto.TechBlogResponse;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TechBlogController.class)
@Import(MessageSourceConfig.class)
@ActiveProfiles("test")
class TechBlogControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private MeterRegistry meterRegistry;

  @MockitoBean private TechBlogQueryService techBlogQueryService;

  @Test
  @DisplayName("GET /techblogs - 테크 블로그 목록 조회 성공")
  void getTechBlogs_shouldReturnList() throws Exception {
    // Given
    TechBlogResponse post =
        new TechBlogResponse(
            1L,
            "우아한형제들",
            "woowahan",
            "테스트 포스트",
            "https://techblog.woowahan.com/1",
            LocalDateTime.now(),
            List.of("Java", "Spring"));
    TechBlogListResponse response = new TechBlogListResponse(List.of(post), "2024-04-13T10:00:00_2", true);

    when(techBlogQueryService.getTechBlogs(any(), anyList(), anyString())).thenReturn(response);

    // When & Then
    mockMvc
        .perform(get("/techblogs").param("cursor", "2024-04-13T10:00:00_1").param("companySlug", "woowahan").param("tag", "Java"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200))
        .andExpect(jsonPath("$.data.items[0].id").value(1))
        .andExpect(jsonPath("$.data.items[0].title").value("테스트 포스트"))
        .andExpect(jsonPath("$.data.nextCursor").value("2024-04-13T10:00:00_2"))
        .andExpect(jsonPath("$.data.hasNext").value(true))
        .andExpect(jsonPath("$.error").isEmpty());
  }

  @Test
  @DisplayName("GET /techblogs - 데이터가 없을 때 빈 목록 반환")
  void getTechBlogs_whenEmpty_shouldReturnEmptyList() throws Exception {
    // Given
    TechBlogListResponse response = new TechBlogListResponse(List.of(), null, false);
    when(techBlogQueryService.getTechBlogs(any(), any(), any())).thenReturn(response);

    // When & Then
    mockMvc
        .perform(get("/techblogs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200))
        .andExpect(jsonPath("$.data.items").isEmpty())
        .andExpect(jsonPath("$.data.nextCursor").isEmpty())
        .andExpect(jsonPath("$.data.hasNext").value(false))
        .andExpect(jsonPath("$.error").isEmpty());
  }

  @Test
  @DisplayName("GET /techblogs/companies - 활성 회사 목록 조회 성공")
  void getActiveCompanies_shouldReturnCompanies() throws Exception {
    // Given
    List<TechBlogCompanyResponse> companies = List.of(
        new TechBlogCompanyResponse("woowahan", "WoowaBros"),
        new TechBlogCompanyResponse("toss", "Toss")
    );
    when(techBlogQueryService.getActiveCompanies()).thenReturn(companies);

    // When & Then
    mockMvc
        .perform(get("/techblogs/companies"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200))
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data[0].slug").value("woowahan"))
        .andExpect(jsonPath("$.data[0].name").value("WoowaBros"))
        .andExpect(jsonPath("$.error").isEmpty());
  }

  @Test
  @DisplayName("POST /techblogs/{id}/click - 클릭 수 증가 성공")
  void incrementClickCount_shouldReturnOk() throws Exception {
    // Given
    Long postId = 1L;
    doNothing().when(techBlogQueryService).incrementClickCount(postId);

    // When & Then
    mockMvc
        .perform(post("/techblogs/{id}/click", postId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200))
        .andExpect(jsonPath("$.data").isEmpty())
        .andExpect(jsonPath("$.error").isEmpty());
  }
}
