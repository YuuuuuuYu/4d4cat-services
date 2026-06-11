package com.services.api.applydays.service;

import com.services.core.common.exception.ErrorCode;
import com.services.core.common.exception.InternalServerException;
import com.services.core.common.exception.NotFoundException;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@RequiredArgsConstructor
public class R2Service {

  private final S3Client s3Client;
  private final S3Presigner s3Presigner;

  @Value("${cloudflare.r2.bucket-name}")
  private String bucketName;

  @Value("${cloudflare.r2.image-path}")
  private String imagePath;

  private String getDatePrefix() {
    return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
  }

  public String generateKey(UUID applicationId, int sequence) {
    return String.format(
        "%s/%s/%s/%s_%d.webp",
        imagePath, getDatePrefix(), applicationId, UUID.randomUUID(), sequence);
  }

  public String generateImageKey(UUID applicationId, UUID imageId) {
    return String.format("%s/%s/%s/%s.webp", imagePath, getDatePrefix(), applicationId, imageId);
  }

  public String generatePresignedUrl(String key, String contentType) {
    PutObjectRequest putObjectRequest =
        PutObjectRequest.builder().bucket(bucketName).key(key).contentType(contentType).build();

    PutObjectPresignRequest presignRequest =
        PutObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(10))
            .putObjectRequest(putObjectRequest)
            .build();

    return s3Presigner.presignPutObject(presignRequest).url().toString();
  }

  public String uploadImage(UUID applicationId, int sequence, MultipartFile file) {
    String key = generateKey(applicationId, sequence);

    try {
      PutObjectRequest putObjectRequest =
          PutObjectRequest.builder()
              .bucket(bucketName)
              .key(key)
              .contentType(file.getContentType())
              .metadata(
                  Map.of(
                      "application-id", applicationId.toString(),
                      "original-filename", file.getOriginalFilename()))
              .build();

      s3Client.putObject(
          putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
      return key;
    } catch (IOException e) {
      throw new InternalServerException(ErrorCode.R2_UPLOAD_FAILED);
    }
  }

  public void deleteImage(String key) {
    try {
      s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(key).build());
    } catch (Exception e) {
      throw new InternalServerException(ErrorCode.R2_DELETE_FAILED);
    }
  }

  public byte[] getImageBytes(String key) {
    try {
      return s3Client
          .getObjectAsBytes(GetObjectRequest.builder().bucket(bucketName).key(key).build())
          .asByteArray();
    } catch (Exception e) {
      throw new NotFoundException(ErrorCode.DATA_NOT_FOUND);
    }
  }
}
