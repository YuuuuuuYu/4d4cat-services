package com.services.api.applydays.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.services.api.applydays.dto.ApplicationRequest;
import com.services.api.applydays.dto.CompanySummaryResponse;
import com.services.api.applydays.dto.MyApplicationsDashboardResponse;
import com.services.api.applydays.service.ApplyDaysCommandService;
import com.services.api.applydays.service.ApplyDaysQueryService;
import com.services.api.common.config.SecurityConfiguration;
import com.services.api.common.security.handler.OAuth2SuccessHandler;
import com.services.api.common.security.jwt.JwtProvider;
import com.services.api.common.security.service.CustomOAuth2UserService;
import com.services.core.applydays.dto.ApplicationDetailResponse;
import com.services.core.applydays.dto.ApplyDaysStatisticsResponse;
import com.services.core.applydays.dto.CompanyListResponse;
import com.services.core.applydays.dto.HiringStepDetail;
import com.services.core.applydays.dto.MyApplicationsSummaryResponse;
import com.services.core.applydays.entity.ApplicationChannel;
import com.services.core.common.dto.PageResponse;
import com.services.core.common.infrastructure.RedisDataStorage;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ApplyDaysController.class)
@Import(SecurityConfiguration.class)
class ApplyDaysControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private ClientRegistrationRepository clientRegistrationRepository;
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
    CompanySummaryResponse response =
        CompanySummaryResponse.builder()
            .slug(slug)
            .name("Naver")
            .companyStats(
                ApplyDaysStatisticsResponse.builder().reviewCount(10).stepStatistics(null).build())
            .build();

    given(applyDaysQueryService.getCompanySummary(any(), eq(slug))).willReturn(response);

    // when & then
    mockMvc
        .perform(get("/applydays/companies/" + slug))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.companyStats.reviewCount").value(10))
        .andExpect(jsonPath("$.data.companyStats.stepStatistics").doesNotExist());
  }

  @Test
  @WithMockUser(roles = "SUBSCRIBER")
  @DisplayName("회사 상세를 조회하면 ApplicationDetailResponse 리스트를 반환한다")
  void get_company_details_success() throws Exception {
    // given
    String slug = "naver";
    ApplicationDetailResponse dto =
        new ApplicationDetailResponse(
            UUID.randomUUID(), slug, null, "Dev", null, null, null, "Engineer", null);

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

  @Test
  @DisplayName("비로그인 상태에서 기업 목록을 조회하면 평균 응답속도와 무통보 관련 수치가 마스킹된다")
  void get_companies_anonymous_masked() throws Exception {
    // given
    CompanyListResponse company =
        CompanyListResponse.builder()
            .slug("naver")
            .name("Naver")
            .reviewCount(10)
            .ghostingCount(null)
            .ghostingRate(null)
            .avgResponseTime(null)
            .build();
    PageResponse<CompanyListResponse> response = new PageResponse<>(List.of(company), false);

    given(applyDaysQueryService.getCompanies(any(), any(), any())).willReturn(response);

    Authentication anonymousAuth =
        new AnonymousAuthenticationToken(
            "key",
            "anonymousUser",
            org.springframework.security.core.authority.AuthorityUtils.createAuthorityList(
                "ROLE_ANONYMOUS"));

    // when & then
    mockMvc
        .perform(get("/applydays/companies?page=0&size=10").with(authentication(anonymousAuth)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].slug").value("naver"))
        .andExpect(jsonPath("$.data.content[0].name").value("Naver"))
        .andExpect(jsonPath("$.data.content[0].reviewCount").value(10))
        .andExpect(jsonPath("$.data.content[0].ghostingCount").doesNotExist())
        .andExpect(jsonPath("$.data.content[0].ghostingRate").doesNotExist())
        .andExpect(jsonPath("$.data.content[0].avgResponseTime").doesNotExist());
  }

  @Test
  @WithMockUser(roles = "USER")
  @DisplayName("로그인 상태에서 기업 목록을 조회하면 요약 통계 정보가 그대로 노출된다")
  void get_companies_authenticated_full() throws Exception {
    // given
    CompanyListResponse company =
        CompanyListResponse.builder()
            .slug("naver")
            .name("Naver")
            .reviewCount(10)
            .ghostingCount(2)
            .ghostingRate(0.2)
            .avgResponseTime("{\"DOCUMENT\":{\"avg\":5.2,\"count\":10}}")
            .build();
    PageResponse<CompanyListResponse> response = new PageResponse<>(List.of(company), false);

    given(applyDaysQueryService.getCompanies(any(), any(), any())).willReturn(response);

    // when & then
    mockMvc
        .perform(get("/applydays/companies?page=0&size=10"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].slug").value("naver"))
        .andExpect(jsonPath("$.data.content[0].name").value("Naver"))
        .andExpect(jsonPath("$.data.content[0].reviewCount").value(10))
        .andExpect(jsonPath("$.data.content[0].ghostingCount").value(2))
        .andExpect(jsonPath("$.data.content[0].ghostingRate").value(0.2))
        .andExpect(
            jsonPath("$.data.content[0].avgResponseTime")
                .value("{\"DOCUMENT\":{\"avg\":5.2,\"count\":10}}"));
  }

  @Test
  @WithMockUser(username = "test@example.com", roles = "USER")
  @DisplayName("대시보드 통합 정보를 조회한다")
  void get_my_dashboard_success() throws Exception {
    // given
    MyApplicationsSummaryResponse summary =
        MyApplicationsSummaryResponse.builder()
            .totalCount(3L)
            .pendingCount(1L)
            .approvedCount(1L)
            .rejectedCount(1L)
            .build();

    MyApplicationsDashboardResponse response =
        MyApplicationsDashboardResponse.builder()
            .summary(summary)
            .pendingApplications(new PageResponse<>(List.of(), false))
            .approvedApplications(new PageResponse<>(List.of(), false))
            .rejectedApplications(new PageResponse<>(List.of(), false))
            .build();

    given(
            applyDaysQueryService.getMyApplicationsDashboard(
                eq("test@example.com"), any(org.springframework.data.domain.Pageable.class)))
        .willReturn(response);

    // when & then
    mockMvc
        .perform(get("/applydays/my/dashboard"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.summary.totalCount").value(3))
        .andExpect(jsonPath("$.data.summary.pendingCount").value(1))
        .andExpect(jsonPath("$.data.pendingApplications.content").isEmpty())
        .andExpect(jsonPath("$.data.approvedApplications.content").isEmpty())
        .andExpect(jsonPath("$.data.rejectedApplications.content").isEmpty());
  }
}
