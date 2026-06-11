package com.services.api.applydays.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.services.api.applydays.dto.CompanySummaryResponse;
import com.services.core.applydays.dto.TimelineBasicResponse;
import com.services.core.applydays.dto.TimelineDetailResponse;
import com.services.core.applydays.entity.Application;
import com.services.core.applydays.entity.ApplyDaysStatistics;
import com.services.core.applydays.entity.VerificationRequest;
import com.services.core.applydays.entity.VerificationStatus;
import com.services.core.applydays.repository.ApplicationRepository;
import com.services.core.applydays.repository.ApplyDaysStatisticsRepository;
import com.services.core.applydays.repository.CategoryRepository;
import com.services.core.applydays.repository.VerificationRequestRepository;
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
            meterRegistry);
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

    // when
    Application result = applyDaysQueryService.viewApplication(email, appId, password);

    // then
    assertThat(result).isNotNull();
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
  @DisplayName("getCompanySummary는 로그인한 USER 권한일 때 COMPANY와 CAT_L1 통계를 가져온다")
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
            .stepStatistics("{}")
            .build();

    ApplyDaysStatistics l1Stat =
        ApplyDaysStatistics.builder()
            .companySlug(companySlug)
            .statType("CAT_L1")
            .categoryId(1L)
            .reviewCount(5)
            .ghostingCount(1)
            .stepStatistics("{}")
            .build();

    ApplyDaysStatistics l2Stat =
        ApplyDaysStatistics.builder()
            .companySlug(companySlug)
            .statType("CAT_L2")
            .categoryId(2L)
            .reviewCount(3)
            .ghostingCount(0)
            .stepStatistics("{}")
            .build();

    when(companyRepository.findBySlug(companySlug)).thenReturn(Optional.of(company));
    when(statisticsRepository.findAllByCompanySlug(companySlug))
        .thenReturn(List.of(companyStat, l1Stat, l2Stat));

    Authentication auth = mock(Authentication.class);
    when(auth.isAuthenticated()).thenReturn(true);
    GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_USER");
    when(auth.getAuthorities()).thenAnswer(invocation -> List.of(authority));

    // when
    CompanySummaryResponse response = applyDaysQueryService.getCompanySummary(auth, companySlug);

    // then
    assertThat(response.getSlug()).isEqualTo(companySlug);
    assertThat(response.getName()).isEqualTo("Naver");
    assertThat(response.getCompanyStats()).isEqualTo(companyStat);
    assertThat(response.getCategoryL1Stats()).containsExactly(l1Stat);
    assertThat(response.getCategoryL2Stats()).isEmpty();
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
  @DisplayName("getCompanyTimeline은 USER 권한일 때 ForbiddenException이 발생한다")
  void getCompanyTimeline_user_forbidden() {
    // given
    String companySlug = "naver";
    Authentication auth = mock(Authentication.class);
    when(auth.isAuthenticated()).thenReturn(true);
    GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_USER");
    when(auth.getAuthorities()).thenAnswer(invocation -> List.of(authority));
    Pageable pageable = PageRequest.of(0, 10);

    // when & then
    assertThatThrownBy(() -> applyDaysQueryService.getCompanyTimeline(auth, companySlug, pageable))
        .isInstanceOf(ForbiddenException.class);
  }

  @Test
  @DisplayName("getCompanyDetails는 SUBSCRIBER 권한일 때 상세 정보를 반환한다")
  void getCompanyDetails_subscriber() {
    // given
    String companySlug = "naver";
    Authentication auth = mock(Authentication.class);
    when(auth.isAuthenticated()).thenReturn(true);
    GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_SUBSCRIBER");
    when(auth.getAuthorities()).thenAnswer(invocation -> List.of(authority));

    List<Application> expected = List.of();
    when(applicationRepository.findAllByCompanySlugAndVerificationStatus(
            companySlug, VerificationStatus.APPROVED))
        .thenReturn(expected);

    // when
    List<Application> result = applyDaysQueryService.getCompanyDetails(auth, companySlug);

    // then
    assertThat(result).isEqualTo(expected);
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
}
