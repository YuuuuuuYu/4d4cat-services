package com.services.api.applydays.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.services.api.applydays.dto.ApplicationRequest;
import com.services.core.applydays.entity.Application;
import com.services.core.applydays.entity.ApplicationChannel;
import com.services.core.applydays.entity.Category;
import com.services.core.applydays.entity.VerificationRequest;
import com.services.core.applydays.repository.ApplicationRepository;
import com.services.core.applydays.repository.CategoryRepository;
import com.services.core.applydays.repository.VerificationImageRepository;
import com.services.core.applydays.repository.VerificationRequestRepository;
import com.services.core.common.persistence.entity.Company;
import com.services.core.common.persistence.entity.member.Member;
import com.services.core.common.persistence.entity.member.Role;
import com.services.core.common.persistence.repository.CompanyRepository;
import com.services.core.common.persistence.repository.member.MemberRepository;
import com.services.core.fixture.ApplyDaysFixtures;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApplyDaysCommandServiceTest {

  @Mock private ApplicationRepository applicationRepository;
  @Mock private CompanyRepository companyRepository;
  @Mock private CategoryRepository categoryRepository;
  @Mock private MemberRepository memberRepository;
  @Mock private VerificationRequestRepository verificationRequestRepository;
  @Mock private VerificationImageRepository verificationImageRepository;
  private MeterRegistry meterRegistry;

  private ApplyDaysCommandService applyDaysCommandService;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    applyDaysCommandService =
        new ApplyDaysCommandService(
            applicationRepository,
            companyRepository,
            categoryRepository,
            memberRepository,
            verificationRequestRepository,
            verificationImageRepository,
            meterRegistry);
  }

  @Test
  @DisplayName("지원 내역을 등록하면 지표가 증가하고 ID가 반환된다")
  void registerApplication_success() {
    // given
    String email = "test@example.com";
    ApplicationRequest request =
        new ApplicationRequest(
            "naver",
            "Naver",
            1L,
            "Backend Developer",
            OffsetDateTime.now(),
            List.of(),
            ApplicationChannel.DIRECT);

    Member member = ApplyDaysFixtures.createMember(email, Role.USER);
    ApplyDaysFixtures.setId(member, UUID.randomUUID());
    Company company = Company.builder().slug("naver").name("Naver").build();
    Category category = Category.builder().name("Dev").depth(1).build();

    when(memberRepository.findByEmail(email)).thenReturn(Optional.of(member));
    when(companyRepository.findBySlug("naver")).thenReturn(Optional.of(company));
    when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
    when(applicationRepository.save(any(Application.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // when
    UUID result = applyDaysCommandService.registerApplication(email, request);

    // then
    assertThat(result).isNotNull();
    verify(applicationRepository).save(any(Application.class));
    verify(verificationRequestRepository).save(any());

    assertThat(meterRegistry.find("applydays.applications.registered").counter()).isNotNull();
    assertThat(meterRegistry.find("applydays.applications.registered").counter().count())
        .isEqualTo(1);
  }

  @Test
  @DisplayName("지원 내역을 삭제하면 지표가 증가한다")
  void deleteApplication_success() {
    // given
    String email = "test@example.com";
    UUID appId = UUID.randomUUID();
    UUID memberId = UUID.randomUUID();

    Member member = ApplyDaysFixtures.createMember(email, Role.USER);
    ApplyDaysFixtures.setId(member, memberId);

    VerificationRequest vr =
        VerificationRequest.builder().applicationId(appId).memberId(memberId).build();

    when(memberRepository.findByEmail(email)).thenReturn(Optional.of(member));
    when(verificationRequestRepository.findByApplicationIdIn(List.of(appId)))
        .thenReturn(List.of(vr));

    // when
    applyDaysCommandService.deleteApplication(email, appId);

    // then
    verify(applicationRepository).deleteAllByIdInBatch(List.of(appId));
    assertThat(meterRegistry.find("applydays.applications.deleted").counter()).isNotNull();
    assertThat(meterRegistry.find("applydays.applications.deleted").counter().count()).isEqualTo(1);
  }
}
