package com.services.api.applydays.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.services.core.applydays.entity.VerificationImage;
import com.services.core.applydays.repository.VerificationImageRepository;
import com.services.core.common.exception.NotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VerificationQueryServiceTest {

  @Mock private VerificationImageRepository verificationImageRepository;
  @Mock private R2Service r2Service;

  @InjectMocks private VerificationQueryService verificationQueryService;

  @Test
  @DisplayName("이미지 ID로 바이트 데이터를 성공적으로 조회한다")
  void getImageBytes_success() {
    // given
    UUID imageId = UUID.randomUUID();
    String imageUrl = "http://r2.endpoint/image.png";
    byte[] expectedBytes = new byte[] {1, 2, 3, 4};

    VerificationImage image =
        VerificationImage.builder().applicationId(UUID.randomUUID()).imageUrl(imageUrl).build();

    when(verificationImageRepository.findById(imageId)).thenReturn(Optional.of(image));
    when(r2Service.getImageBytes(imageUrl)).thenReturn(expectedBytes);

    // when
    byte[] result = verificationQueryService.getImageBytes(imageId);

    // then
    assertThat(result).isEqualTo(expectedBytes);
    verify(verificationImageRepository).findById(imageId);
    verify(r2Service).getImageBytes(imageUrl);
  }

  @Test
  @DisplayName("존재하지 않는 이미지 ID로 조회 시 NotFoundException이 발생한다")
  void getImageBytes_notFound() {
    // given
    UUID imageId = UUID.randomUUID();
    when(verificationImageRepository.findById(imageId)).thenReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> verificationQueryService.getImageBytes(imageId))
        .isInstanceOf(NotFoundException.class);
  }
}
