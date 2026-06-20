package com.services.data.applydays.worker;

import com.services.core.applydays.repository.DeletedImageProjection;
import com.services.core.applydays.repository.VerificationImageRepository;
import com.services.core.common.notification.discord.NotifyDiscord;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

@Slf4j
@Component
@ConditionalOnProperty(
    name = "app.scheduler.enabled.verification-image-cleanup",
    havingValue = "true",
    matchIfMissing = true)
public class VerificationImageCleanupScheduler {

  private final String lockKey;
  private final Duration lockDuration;
  private final String bucketName;
  private final VerificationImageRepository verificationImageRepository;
  private final S3Client s3Client;
  private final RedisTemplate<String, Object> redisTemplate;

  public VerificationImageCleanupScheduler(
      @Value("${app.scheduler.lock.verification-image-cleanup.key:lock:verification-image-cleanup}") String lockKey,
      @Value("${app.scheduler.lock.verification-image-cleanup.duration:10m}") Duration lockDuration,
      @Value("${cloudflare.r2.bucket-name}") String bucketName,
      VerificationImageRepository verificationImageRepository,
      S3Client s3Client,
      RedisTemplate<String, Object> redisTemplate) {
    this.lockKey = lockKey;
    this.lockDuration = lockDuration;
    this.bucketName = bucketName;
    this.verificationImageRepository = verificationImageRepository;
    this.s3Client = s3Client;
    this.redisTemplate = redisTemplate;
  }

  @Scheduled(cron = "0 0 3 * * *")
  @NotifyDiscord(taskName = "Verification Image Cleanup")
  @Transactional
  public void cleanupDeletedImages() {
    Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "locked", lockDuration);
    if (acquired == null || !acquired) {
      log.info("Another instance is already running Verification Image Cleanup. Skipping execution.");
      return;
    }

    log.info("Starting scheduled cleanup of verification images of deleted applications.");

    List<DeletedImageProjection> deletedImages =
        verificationImageRepository.findAllByApplicationDeletedTrue();

    if (deletedImages.isEmpty()) {
      log.info("No verification images of deleted applications found for cleanup.");
      return;
    }

    log.info("Found {} verification images of deleted applications to clean up.", deletedImages.size());

    for (DeletedImageProjection image : deletedImages) {
      try {
        s3Client.deleteObject(
            DeleteObjectRequest.builder().bucket(bucketName).key(image.getImageUrl()).build());
        log.debug("Successfully deleted image from R2: {}", image.getImageUrl());
      } catch (Exception e) {
        log.error(
            "Failed to delete image from R2: {}. Skipping DB hard-delete.", image.getImageUrl(), e);
      }
    }

    List<UUID> idsToHardDelete =
        deletedImages.stream().map(DeletedImageProjection::getId).toList();

    if (!idsToHardDelete.isEmpty()) {
      verificationImageRepository.deleteAllByIdInBatch(idsToHardDelete);
    }

    log.info(
        "Finished scheduled cleanup. Hard-deleted {} records from database.",
        idsToHardDelete.size());
  }
}
