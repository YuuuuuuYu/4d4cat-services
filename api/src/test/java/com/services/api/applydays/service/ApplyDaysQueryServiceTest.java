package com.services.api.applydays.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.services.api.applydays.dto.CompanySummaryResponse;
import com.services.api.applydays.dto.MyApplicationsDashboardResponse;
import com.services.api.common.security.service.MemberService;
import com.services.core.applydays.dto.ApplicationDetailResponse;
import com.services.core.applydays.dto.CompanyListResponse;
import com.services.core.applydays.dto.TimelineBasicResponse;
import com.services.core.applydays.dto.TimelineDetailResponse;
import com.services.core.applydays.entity.Application;
import com.services.core.applydays.entity.ApplyDaysStatistics;
import com.services.core.applydays.entity.Category;
import com.services.core.applydays.entity.VerificationRequest;
import com.services.core.applydays.entity.VerificationStatus;
import com.services.core.applydays.repository.ApplicationRepository;
import com.services.core.applydays.repository.ApplyDaysStatisticsRepository;
import com.services.core.applydays.repository.CategoryRepository;
import com.services.core.applydays.repository.VerificationRequestRepository;
import com.services.core.common.dto.PageResponse;
import com.services.core.common.exception.BadRequestException;
import com.services.core.common.exception.ForbiddenException;
import com.services.core.common.persistence.entity.Company;
import com.services.core.common.persistence.entity.member.Member;
import com.services.core.common.persistence.entity.member.Role;
import com.services.core.common.persistence.repository.CompanyRepository;
import com.services.core.common.persistence.repository.member.MemberRepository;
import com.services.core.fixture.ApplyDaysFixtures;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
class ApplyDaysQueryServiceTest {

  @Mock private ApplicationRepository applicationRepository;
  @Mock private CompanyRepository companyRepository;
  @Mock private CategoryRepository categoryRepository;
  @Mock private MemberRepository memberRepository;
  @Mock private VerificationRequestRepository verificationRequestRepository;
  @Mock private ApplyDaysStatisticsRepository statisticsRepository;
  @Mock private MemberService memberService;
  private MeterRegistry meterRegistry;

  private ApplyDaysQueryService applyDaysQueryService;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    applyDaysQueryService =
        new ApplyDaysQueryService(
            applicationRepository,
            companyRepository,
            categoryRepository,
            memberRepository,
            verificationRequestRepository,
            statisticsRepository,
            meterRegistry,
            memberService);
  }

  @Test
  @DisplayName("비밀번호가 일치하면 지원서를 조회할 수 있고 지표가 증가한다")
  void viewApplication_success() {
    // given
    String email = "test@example.com";
    UUID appId = UUID.randomUUID();
    UUID memberId = UUID.randomUUID();
    String password = "test-password";

    Application application = ApplyDaysFixtures.createApplication("naver", 1L);
    application.setAccessPassword(password);

    Member member = ApplyDaysFixtures.createMember(email, Role.USER);
    ApplyDaysFixtures.setId(member, memberId);

    VerificationRequest vr =
        VerificationRequest.builder().applicationId(appId).memberId(memberId).build();

    when(applicationRepository.findById(appId)).thenReturn(Optional.of(application));
    when(memberRepository.findByEmail(email)).thenReturn(Optional.of(member));
    when(verificationRequestRepository.findByApplicationId(appId)).thenReturn(Optional.of(vr));
    when(categoryRepository.findById(1L))
        .thenReturn(Optional.of(new Category("Developer", null, 1)));

    // when
    ApplicationDetailResponse result =
        applyDaysQueryService.viewApplication(email, appId, password);

    // then
    assertThat(result).isNotNull();
    assertThat(result.categoryName()).isEqualTo("Developer");
    assertThat(meterRegistry.find("applydays.applications.viewed").counter()).isNotNull();
    assertThat(meterRegistry.find("applydays.applications.viewed").counter().count()).isEqualTo(1);
  }

  @Test
  @DisplayName("비밀번호가 일치하지 않으면 예외가 발생한다")
  void viewApplication_wrongPassword_fail() {
    // given
    UUID appId = UUID.randomUUID();
    String password = "wrong-password";

    Application application = ApplyDaysFixtures.createApplication("naver", 1L);
    application.setAccessPassword("correct-password");

    when(applicationRepository.findById(appId)).thenReturn(Optional.of(application));

    // when & then
    assertThatThrownBy(() -> applyDaysQueryService.viewApplication("any@test.com", appId, password))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  @DisplayName("getCompanySummary는 로그인한 USER 권한일 때 COMPANY와 CAT_L1 통계를 가져오며 stepStatistics는 제외된다")
  void getCompanySummary_user() {
    // given
    String companySlug = "naver";
    Company company = Company.builder().slug(companySlug).name("Naver").build();

    ApplyDaysStatistics companyStat =
        ApplyDaysStatistics.builder()
            .companySlug(companySlug)
            .statType("COMPANY")
            .categoryId(null)
            .reviewCount(10)
            .ghostingCount(2)
            .stepStatistics("{\"detail\": \"secret\"}")
            .build();

    ApplyDaysStatistics l1Stat =
        ApplyDaysStatistics.builder()
            .companySlug(companySlug)
            .statType("CAT_L1")
            .categoryId(1L)
            .reviewCount(5)
            .ghostingCount(1)
            .stepStatistics("{\"detail\": \"secret\"}")
            .build();

    ApplyDaysStatistics l2Stat =
        ApplyDaysStatistics.builder()
            .companySlug(companySlug)
            .statType("CAT_L2")
            .categoryId(2L)
            .reviewCount(3)
            .ghostingCount(0)
            .stepStatistics("{\"detail\": \"secret\"}")
            .build();

    when(companyRepository.findBySlug(companySlug)).thenReturn(Optional.of(company));
    when(statisticsRepository.findAllByCompanySlug(companySlug))
        .thenReturn(List.of(companyStat, l1Stat, l2Stat));
    when(categoryRepository.findAllByOrderByNameAsc())
        .thenReturn(
            List.of(
                new Category("L1 Cat", null, 1) {
                  {
                    ApplyDaysFixtures.setId(this, 1L);
                  }
                },
                new Category("L2 Cat", 1L, 2) {
                  {
                    ApplyDaysFixtures.setId(this, 2L);
                  }
                }));

    Authentication auth = mock(Authentication.class);
    when(auth.isAuthenticated()).thenReturn(true);
    GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_USER");
    when(auth.getAuthorities()).thenAnswer(invocation -> List.of(authority));

    // when
    CompanySummaryResponse response = applyDaysQueryService.getCompanySummary(auth, companySlug);

    // then
    assertThat(response.getSlug()).isEqualTo(companySlug);
    assertThat(response.getName()).isEqualTo("Naver");
    assertThat(response.getCompanyStats().getReviewCount()).isEqualTo(10);
    assertThat(response.getCompanyStats().getStepStatistics())
        .isEqualTo("{\"detail\": \"secret\"}");
    assertThat(response.getCategoryL1Stats()).hasSize(1);
    assertThat(response.getCategoryL1Stats().get(0).getCategoryName()).isEqualTo("L1 Cat");
    assertThat(response.getCategoryL1Stats().get(0).getStepStatistics())
        .isEqualTo("{\"detail\": \"secret\"}");
    assertThat(response.getCategoryL2Stats()).isEmpty();
  }

  @Test
  @DisplayName("getCompanySummary는 SUBSCRIBER 권한일 때 모든 통계와 stepStatistics를 포함한다")
  void getCompanySummary_subscriber() {
    // given
    String companySlug = "naver";
    Company company = Company.builder().slug(companySlug).name("Naver").build();

    ApplyDaysStatistics companyStat =
        ApplyDaysStatistics.builder()
            .companySlug(companySlug)
            .statType("COMPANY")
            .categoryId(null)
            .reviewCount(10)
            .ghostingCount(2)
            .stepStatistics("{\"detail\": \"secret\"}")
            .build();

    when(companyRepository.findBySlug(companySlug)).thenReturn(Optional.of(company));
    when(statisticsRepository.findAllByCompanySlug(companySlug)).thenReturn(List.of(companyStat));
    when(categoryRepository.findAllByOrderByNameAsc()).thenReturn(List.of());

    Authentication auth = mock(Authentication.class);
    when(auth.isAuthenticated()).thenReturn(true);
    GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_SUBSCRIBER");
    when(auth.getAuthorities()).thenAnswer(invocation -> List.of(authority));

    // when
    CompanySummaryResponse response = applyDaysQueryService.getCompanySummary(auth, companySlug);

    // then
    assertThat(response.getCompanyStats().getStepStatistics())
        .isEqualTo("{\"detail\": \"secret\"}");
  }

  @Test
  @DisplayName("getCompanyTimeline은 SUBSCRIBER 권한일 때 상세 타임라인을 반환한다")
  void getCompanyTimeline_subscriber() {
    // given
    String companySlug = "naver";
    Authentication auth = mock(Authentication.class);
    when(auth.isAuthenticated()).thenReturn(true);
    GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_SUBSCRIBER");
    when(auth.getAuthorities()).thenAnswer(invocation -> List.of(authority));
    Pageable pageable = PageRequest.of(0, 10);

    Slice<TimelineDetailResponse> expected = new SliceImpl<>(List.of(), pageable, false);
    when(applicationRepository.findTimelineDetailByCompanySlug(companySlug, pageable))
        .thenReturn(expected);

    // when
    Slice<? extends TimelineBasicResponse> result =
        applyDaysQueryService.getCompanyTimeline(auth, companySlug, pageable);

    // then
    assertThat(result).isEqualTo(expected);
  }

  @Test
  @DisplayName("getCompanyTimeline은 USER 권한일 때 빈 슬라이스를 반환한다")
  void getCompanyTimeline_user_empty() {
    // given
    String companySlug = "naver";
    Authentication auth = mock(Authentication.class);
    when(auth.isAuthenticated()).thenReturn(true);
    GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_USER");
    when(auth.getAuthorities()).thenAnswer(invocation -> List.of(authority));
    Pageable pageable = PageRequest.of(0, 10);

    // when
    Slice<? extends TimelineBasicResponse> result =
        applyDaysQueryService.getCompanyTimeline(auth, companySlug, pageable);

    // then
    assertThat(result.getContent()).isEmpty();
  }

  @Test
  @DisplayName("getCompanyDetails는 SUBSCRIBER 권한일 때 상세 정보를 DTO 형식으로 반환한다")
  void getCompanyDetails_subscriber() {
    // given
    String companySlug = "naver";
    Authentication auth = mock(Authentication.class);
    when(auth.isAuthenticated()).thenReturn(true);
    GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_SUBSCRIBER");
    when(auth.getAuthorities()).thenAnswer(invocation -> List.of(authority));

    Application app = ApplyDaysFixtures.createApplication(companySlug, 1L);
    when(applicationRepository.findAllByCompanySlugAndVerificationStatus(
            companySlug, VerificationStatus.APPROVED))
        .thenReturn(List.of(app));
    when(categoryRepository.findAllByOrderByNameAsc())
        .thenReturn(
            List.of(
                new Category("Developer", null, 1) {
                  {
                    ApplyDaysFixtures.setId(this, 1L);
                  }
                }));

    // when
    List<ApplicationDetailResponse> result =
        applyDaysQueryService.getCompanyDetails(auth, companySlug);

    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).categoryName()).isEqualTo("Developer");
    assertThat(result.get(0).companySlug()).isEqualTo(companySlug);
  }

  @Test
  @DisplayName("getCompanyDetails는 USER 권한일 때 ForbiddenException이 발생한다")
  void getCompanyDetails_user_forbidden() {
    // given
    String companySlug = "naver";
    Authentication auth = mock(Authentication.class);
    when(auth.isAuthenticated()).thenReturn(true);
    GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_USER");
    when(auth.getAuthorities()).thenAnswer(invocation -> List.of(authority));

    // when & then
    assertThatThrownBy(() -> applyDaysQueryService.getCompanyDetails(auth, companySlug))
        .isInstanceOf(ForbiddenException.class);
  }

  @Test
  @DisplayName("getCompanies는 비로그인 상태일 때 요약 통계 정보를 마스킹하여 반환한다")
  void getCompanies_anonymous_masked() {
    // given
    String query = "naver";
    Pageable pageable = PageRequest.of(0, 10);
    CompanyListResponse rawCompany =
        CompanyListResponse.builder()
            .slug("naver")
            .name("Naver")
            .reviewCount(5)
            .ghostingCount(1)
            .ghostingRate(0.2)
            .avgResponseTime(
                "{\"DOCUMENT\":{\"avg\":3.5,\"count\":2},\"CODING\":{\"avg\":5.0,\"count\":3}}")
            .build();

    Slice<CompanyListResponse> slice = new SliceImpl<>(List.of(rawCompany), pageable, false);
    when(companyRepository.findAllVerifiedWithStats(eq(query), any(Pageable.class)))
        .thenReturn(slice);

    Authentication auth = mock(Authentication.class);
    when(auth.isAuthenticated()).thenReturn(false);

    // when
    PageResponse<CompanyListResponse> response =
        applyDaysQueryService.getCompanies(auth, query, pageable);

    // then
    assertThat(response.getContent()).hasSize(1);
    CompanyListResponse masked = response.getContent().get(0);
    assertThat(masked.getSlug()).isEqualTo("naver");
    assertThat(masked.getName()).isEqualTo("Naver");
    assertThat(masked.getReviewCount()).isEqualTo(5);
    assertThat(masked.getGhostingCount()).isNull();
    assertThat(masked.getGhostingRate()).isNull();
    assertThat(masked.getAvgResponseTime()).isNull();
  }

  @Test
  @DisplayName("getCompanies는 로그인 상태일 때 요약 통계 정보를 그대로 반환한다")
  void getCompanies_authenticated_full() {
    // given
    String query = "naver";
    Pageable pageable = PageRequest.of(0, 10);
    CompanyListResponse rawCompany =
        CompanyListResponse.builder()
            .slug("naver")
            .name("Naver")
            .reviewCount(5)
            .ghostingCount(1)
            .ghostingRate(0.2)
            .avgResponseTime("{\"DOCUMENT\":{\"avg\":3.5,\"count\":2}}")
            .build();

    Slice<CompanyListResponse> slice = new SliceImpl<>(List.of(rawCompany), pageable, false);
    when(companyRepository.findAllVerifiedWithStats(eq(query), any(Pageable.class)))
        .thenReturn(slice);

    Authentication auth = mock(Authentication.class);
    when(auth.isAuthenticated()).thenReturn(true);
    GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_USER");
    when(auth.getAuthorities()).thenAnswer(invocation -> List.of(authority));

    // when
    PageResponse<CompanyListResponse> response =
        applyDaysQueryService.getCompanies(auth, query, pageable);

    // then
    assertThat(response.getContent()).hasSize(1);
    CompanyListResponse full = response.getContent().get(0);
    assertThat(full.getSlug()).isEqualTo("naver");
    assertThat(full.getReviewCount()).isEqualTo(5);
    assertThat(full.getGhostingCount()).isEqualTo(1);
    assertThat(full.getGhostingRate()).isEqualTo(0.2);
    assertThat(full.getAvgResponseTime()).contains("\"avg\":3.5");
  }

  @Test
  @DisplayName("getMyApplicationsDashboard는 회원의 전체 대시보드 요약 및 상태별 목록을 반환한다")
  void getMyApplicationsDashboard_success() {
    // given
    String email = "test@example.com";
    UUID memberId = UUID.randomUUID();
    Pageable pageable = PageRequest.of(0, 10);

    when(memberService.getMemberIdByEmail(email)).thenReturn(memberId.toString());

    // Mock countByVerificationStatusForMember
    List<Object[]> summaryResults =
        List.of(
            new Object[] {VerificationStatus.PENDING, 2L},
            new Object[] {VerificationStatus.APPROVED, 1L});
    when(applicationRepository.countByVerificationStatusForMember(memberId))
        .thenReturn(summaryResults);

    // Mock findDashboardApplications
    when(applicationRepository.findDashboardApplications(eq(memberId))).thenReturn(List.of());

    // when
    MyApplicationsDashboardResponse dashboard =
        applyDaysQueryService.getMyApplicationsDashboard(email, pageable);

    // then
    assertThat(dashboard).isNotNull();
    assertThat(dashboard.getSummary().totalCount()).isEqualTo(3L);
    assertThat(dashboard.getSummary().pendingCount()).isEqualTo(2L);
    assertThat(dashboard.getSummary().approvedCount()).isEqualTo(1L);
    assertThat(dashboard.getSummary().rejectedCount()).isEqualTo(0L);
    assertThat(dashboard.getPendingApplications().getContent()).isEmpty();
    assertThat(dashboard.getApprovedApplications().getContent()).isEmpty();
    assertThat(dashboard.getRejectedApplications().getContent()).isEmpty();
  }
}
