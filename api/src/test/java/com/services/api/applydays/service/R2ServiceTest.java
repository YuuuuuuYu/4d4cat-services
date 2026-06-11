package com.services.api.applydays.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@ExtendWith(MockitoExtension.class)
class R2ServiceTest {

  @Mock private S3Client s3Client;

  @InjectMocks private R2Service r2Service;

  private final String bucketName = "test-bucket";
  private final String imagePath = "test-images";

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(r2Service, "bucketName", bucketName);
    ReflectionTestUtils.setField(r2Service, "imagePath", imagePath);
  }

  private String getDatePrefix() {
    return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
  }

  @Test
  @DisplayName("generateImageKey는 올바른 형식의 키를 생성한다.")
  void generateImageKey_returns_correct_format() {
    // given
    UUID applicationId = UUID.randomUUID();
    UUID imageId = UUID.randomUUID();

    // when
    String key = r2Service.generateImageKey(applicationId, imageId);

    // then
    assertThat(key)
        .isEqualTo(
            String.format("%s/%s/%s/%s.webp", imagePath, getDatePrefix(), applicationId, imageId));
  }

  @Test
  @DisplayName("uploadImage는 정식 경로에 파일을 업로드하고 키를 반환한다.")
  void uploadImage_success_returns_key() throws IOException {
    // given
    UUID applicationId = UUID.randomUUID();
    int sequence = 1;
    MultipartFile file = mock(MultipartFile.class);
    when(file.getOriginalFilename()).thenReturn("test.jpg");
    when(file.getContentType()).thenReturn("image/jpeg");
    when(file.getSize()).thenReturn(1024L);
    when(file.getInputStream()).thenReturn(mock(InputStream.class));

    // when
    String key = r2Service.uploadImage(applicationId, sequence, file);

    // then
    assertThat(key).startsWith(imagePath + "/" + getDatePrefix() + "/" + applicationId + "/");
    assertThat(key).endsWith("_" + sequence + ".webp");
    verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
  }
}
