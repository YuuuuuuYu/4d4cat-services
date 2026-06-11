package com.services.api.applydays.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.services.api.applydays.dto.PresignedUrlResponse;
import com.services.core.applydays.entity.Application;
import com.services.core.applydays.entity.VerificationImage;
import com.services.core.applydays.entity.VerificationRequest;
import com.services.core.applydays.repository.ApplicationRepository;
import com.services.core.applydays.repository.VerificationImageRepository;
import com.services.core.applydays.repository.VerificationRequestRepository;
import com.services.core.common.persistence.entity.member.Member;
import com.services.core.common.persistence.entity.member.Role;
import com.services.core.common.persistence.repository.member.MemberRepository;
import com.services.core.fixture.ApplyDaysFixtures;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class VerificationServiceTest {

  @Mock private ApplicationRepository applicationRepository;
  @Mock private VerificationImageRepository verificationImageRepository;
  @Mock private VerificationRequestRepository verificationRequestRepository;
  @Mock private MemberRepository memberRepository;
  @Mock private R2Service r2Service;
  private MeterRegistry meterRegistry;

  private VerificationCommandService verificationCommandService;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    verificationCommandService =
        new VerificationCommandService(
            applicationRepository,
            verificationImageRepository,
            verificationRequestRepository,
            memberRepository,
            r2Service,
            meterRegistry);
  }

  @Test
  @DisplayName("Presigned URL 발급 요청 시 URL을 생성하고 이미지 레코드를 저장한다.")
  void getPresignedUrl_success() {
    // given
    String email = "test@example.com";
    UUID applicationId = UUID.randomUUID();
    String fileName = "test.png";
    String contentType = "image/png";

    Application application = ApplyDaysFixtures.createApplication("naver", 1L);
    ApplyDaysFixtures.setId(application, applicationId);
    Member member = ApplyDaysFixtures.createMember(email, Role.USER);
    UUID memberId = UUID.randomUUID();
    ApplyDaysFixtures.setId(member, memberId);
    VerificationRequest vr =
        VerificationRequest.builder().applicationId(applicationId).memberId(memberId).build();

    when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
    when(memberRepository.findByEmail(email)).thenReturn(Optional.of(member));
    when(verificationRequestRepository.findByApplicationId(applicationId))
        .thenReturn(Optional.of(vr));
    when(verificationImageRepository.countByApplicationId(applicationId)).thenReturn(0L);
    when(r2Service.generateKey(eq(applicationId), anyInt())).thenReturn("generated/key");
    when(r2Service.generatePresignedUrl(eq("generated/key"), eq(contentType)))
        .thenReturn("http://presigned.url");
    when(verificationImageRepository.save(any(VerificationImage.class)))
        .thenAnswer(
            i -> {
              VerificationImage vi = i.getArgument(0);
              ApplyDaysFixtures.setId(vi, UUID.randomUUID());
              return vi;
            });

    // when
    PresignedUrlResponse response =
        verificationCommandService.getPresignedUrl(email, applicationId, fileName, contentType);

    // then
    assertThat(response.presignedUrl()).isEqualTo("http://presigned.url");
    assertThat(response.key()).isEqualTo("generated/key");
    assertThat(response.imageId()).isNotNull();
    verify(verificationImageRepository).save(any(VerificationImage.class));
    assertThat(meterRegistry.find("applydays.verification.presigned_url.issued").counter())
        .isNotNull();
    assertThat(meterRegistry.find("applydays.verification.presigned_url.issued").counter().count())
        .isEqualTo(1);
  }

  @Test
  @DisplayName("정상적인 업로드 요청 시 이미지를 저장하고 인증 요청을 생성한다.")
  void uploadVerificationImage_success() {
    // given
    String email = "test@example.com";
    UUID applicationId = UUID.randomUUID();
    MultipartFile file = mock(MultipartFile.class);
    Application application = ApplyDaysFixtures.createApplication("naver", 1L);
    ApplyDaysFixtures.setId(application, applicationId);
    Member member = ApplyDaysFixtures.createMember(email, Role.USER);
    UUID memberId = UUID.randomUUID();
    ApplyDaysFixtures.setId(member, memberId);
    VerificationRequest vr =
        VerificationRequest.builder().applicationId(applicationId).memberId(memberId).build();

    when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
    when(memberRepository.findByEmail(email)).thenReturn(Optional.of(member));
    when(verificationRequestRepository.findByApplicationId(applicationId))
        .thenReturn(Optional.of(vr));
    when(verificationImageRepository.countByApplicationId(applicationId)).thenReturn(0L);
    when(r2Service.uploadImage(eq(applicationId), anyInt(), eq(file))).thenReturn("test/url");
    when(verificationImageRepository.save(any(VerificationImage.class)))
        .thenAnswer(
            i -> {
              VerificationImage vi = i.getArgument(0);
              ApplyDaysFixtures.setId(vi, UUID.randomUUID());
              return vi;
            });

    // when
    UUID resultId = verificationCommandService.uploadVerificationImage(email, applicationId, file);

    // then
    assertThat(resultId).isNotNull();
    verify(r2Service).uploadImage(eq(applicationId), anyInt(), eq(file));
    verify(verificationImageRepository).save(any(VerificationImage.class));
    assertThat(meterRegistry.find("applydays.verification.images.uploaded").counter()).isNotNull();
    assertThat(meterRegistry.find("applydays.verification.images.uploaded").counter().count())
        .isEqualTo(1);
  }
}
