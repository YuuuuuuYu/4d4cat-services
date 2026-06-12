package com.services.api.applydays.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.services.api.applydays.dto.ApplicationRequest;
import com.services.api.applydays.dto.CompanySummaryResponse;
import com.services.api.applydays.service.ApplyDaysCommandService;
import com.services.api.applydays.service.ApplyDaysQueryService;
import com.services.api.common.security.handler.OAuth2SuccessHandler;
import com.services.api.common.security.jwt.JwtProvider;
import com.services.api.common.security.service.CustomOAuth2UserService;
import com.services.core.applydays.dto.ApplicationDetailDto;
import com.services.core.applydays.dto.ApplyDaysStatisticsDto;
import com.services.core.applydays.dto.HiringStepDetail;
import com.services.core.applydays.entity.ApplicationChannel;
import com.services.core.common.infrastructure.RedisDataStorage;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApplyDaysController.class)
class ApplyDaysControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private ApplyDaysCommandService applyDaysCommandService;
  @MockitoBean private ApplyDaysQueryService applyDaysQueryService;

  @MockitoBean private CustomOAuth2UserService customOAuth2UserService;
  @MockitoBean private OAuth2SuccessHandler oAuth2SuccessHandler;
  @MockitoBean private JwtProvider jwtProvider;
  @MockitoBean private RedisDataStorage redisDataStorage;
  @MockitoBean private MeterRegistry meterRegistry;

  @Test
  @WithMockUser(username = "test@example.com", roles = "USER")
  @DisplayName("지원 내역을 등록한다")
  void register_application_success() throws Exception {
    // given
    String email = "test@example.com";
    UUID applicationId = UUID.randomUUID();
    ApplicationRequest request =
        new ApplicationRequest(
            "naver",
            "Naver",
            1L,
            "Software Engineer",
            OffsetDateTime.now(),
            List.of(
                HiringStepDetail.builder()
                    .stepType("CODING")
                    .status("PASSED")
                    .stepDate("2025-01-01")
                    .build()),
            ApplicationChannel.DIRECT);

    given(applyDaysCommandService.registerApplication(eq(email), any(ApplicationRequest.class)))
        .willReturn(applicationId);

    // when & then
    mockMvc
        .perform(
            post("/applydays/applications")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(201))
        .andExpect(jsonPath("$.data").value(applicationId.toString()));
  }

  @Test
  @WithMockUser(roles = "USER")
  @DisplayName("회사 요약을 조회하면 DTO 형식으로 응답하며 stepStatistics는 마스킹된다")
  void get_company_summary_success() throws Exception {
    // given
    String slug = "naver";
    CompanySummaryResponse response = CompanySummaryResponse.builder()
        .slug(slug)
        .name("Naver")
        .companyStats(ApplyDaysStatisticsDto.builder().reviewCount(10).stepStatistics(null).build())
        .build();

    given(applyDaysQueryService.getCompanySummary(any(), eq(slug))).willReturn(response);

    // when & then
    mockMvc
        .perform(get("/applydays/companies/" + slug))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.companyStats.reviewCount").value(10))
        .andExpect(jsonPath("$.data.companyStats.stepStatistics").isEmpty());
  }

  @Test
  @WithMockUser(roles = "SUBSCRIBER")
  @DisplayName("회사 상세를 조회하면 ApplicationDetailDto 리스트를 반환한다")
  void get_company_details_success() throws Exception {
    // given
    String slug = "naver";
    ApplicationDetailDto dto = ApplicationDetailDto.builder()
        .id(UUID.randomUUID())
        .companySlug(slug)
        .categoryName("Dev")
        .positionDetail("Engineer")
        .build();

    given(applyDaysQueryService.getCompanyDetails(any(), eq(slug))).willReturn(List.of(dto));

    // when & then
    mockMvc
        .perform(get("/applydays/companies/" + slug + "/details"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].categoryName").value("Dev"))
        .andExpect(jsonPath("$.data[0].positionDetail").value("Engineer"))
        .andExpect(jsonPath("$.data[0].accessPassword").doesNotExist());
  }
}
