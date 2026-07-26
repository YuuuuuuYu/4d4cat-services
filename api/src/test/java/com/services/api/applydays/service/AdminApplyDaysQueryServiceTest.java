package com.services.api.applydays.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.services.core.applydays.dto.AdminApplicationDetailResponse;
import com.services.core.applydays.dto.AdminApplicationResponse;
import com.services.core.applydays.dto.AdminPendingRequestResponse;
import com.services.core.applydays.entity.VerificationStatus;
import com.services.core.applydays.repository.ApplicationRepository;
import com.services.core.applydays.repository.NotificationQueueRepository;
import com.services.core.applydays.repository.VerificationRequestRepository;
import com.services.core.common.exception.NotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

@ExtendWith(MockitoExtension.class)
class AdminApplyDaysQueryServiceTest {

  @Mock private ApplicationRepository applicationRepository;
  @Mock private VerificationRequestRepository verificationRequestRepository;
  @Mock private NotificationQueueRepository notificationQueueRepository;

  @InjectMocks private AdminApplyDaysQueryService adminApplyDaysQueryService;

  @Test
  @DisplayName("대기 중인 알림 큐 수량을 반환한다")
  void getPendingNotificationCount_success() {
    // given
    when(notificationQueueRepository.countByStatus("PENDING")).thenReturn(5);

    // when
    int count = adminApplyDaysQueryService.getPendingNotificationCount();

    // then
    assertThat(count).isEqualTo(5);
    verify(notificationQueueRepository).countByStatus("PENDING");
  }

  @Test
  @DisplayName("대기 중인 인증 요청 목록을 슬라이스로 조회한다")
  void getPendingRequests_success() {
    // given
    Pageable pageable = PageRequest.of(0, 10);
    AdminPendingRequestResponse mockResponse = mock(AdminPendingRequestResponse.class);
    Slice<AdminPendingRequestResponse> mockSlice = new SliceImpl<>(List.of(mockResponse));

    when(verificationRequestRepository.findAllWithDetailsByStatus(
            VerificationStatus.PENDING, pageable))
        .thenReturn(mockSlice);

    // when
    Slice<AdminPendingRequestResponse> result =
        adminApplyDaysQueryService.getPendingRequests(pageable);

    // then
    assertThat(result.getContent()).hasSize(1);
    verify(verificationRequestRepository)
        .findAllWithDetailsByStatus(VerificationStatus.PENDING, pageable);
  }

  @Test
  @DisplayName("지원서 목록을 조건에 따라 페이징 조회한다")
  void getAllApplications_success() {
    // given
    Pageable pageable = PageRequest.of(0, 10);
    LocalDateTime now = LocalDateTime.now();
    AdminApplicationResponse mockResponse = mock(AdminApplicationResponse.class);
    Slice<AdminApplicationResponse> mockSlice = new SliceImpl<>(List.of(mockResponse));

    when(applicationRepository.findAllWithDetails("naver", now, pageable)).thenReturn(mockSlice);

    // when
    Slice<AdminApplicationResponse> result =
        adminApplyDaysQueryService.getAllApplications("naver", now, pageable);

    // then
    assertThat(result.getContent()).hasSize(1);
    verify(applicationRepository).findAllWithDetails("naver", now, pageable);
  }

  @Test
  @DisplayName("지원서 상세 정보를 조회한다")
  void getApplicationDetail_success() {
    // given
    UUID applicationId = UUID.randomUUID();
    AdminApplicationDetailResponse mockResponse = mock(AdminApplicationDetailResponse.class);

    when(applicationRepository.findApplicationDetailById(applicationId))
        .thenReturn(Optional.of(mockResponse));

    // when
    AdminApplicationDetailResponse result =
        adminApplyDaysQueryService.getApplicationDetail(applicationId);

    // then
    assertThat(result).isNotNull();
    verify(applicationRepository).findApplicationDetailById(applicationId);
  }

  @Test
  @DisplayName("존재하지 않는 지원서 상세 조회 시 NotFoundException이 발생한다")
  void getApplicationDetail_notFound() {
    // given
    UUID applicationId = UUID.randomUUID();

    when(applicationRepository.findApplicationDetailById(applicationId))
        .thenReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> adminApplyDaysQueryService.getApplicationDetail(applicationId))
        .isInstanceOf(NotFoundException.class);
  }
}
