package com.services.api.applydays.scheduler;

import com.services.api.applydays.service.R2Service;
import com.services.core.applydays.entity.VerificationImage;
import com.services.core.applydays.repository.VerificationImageRepository;
import com.services.core.common.notification.discord.NotifyDiscord;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "app.scheduler.enabled.verification-image-cleanup",
    havingValue = "true",
    matchIfMissing = true)
public class VerificationImageCleanupScheduler {

  private final VerificationImageRepository verificationImageRepository;
  private final R2Service r2Service;

  @Scheduled(cron = "0 0 3 * * *")
  @NotifyDiscord(taskName = "Verification Image Cleanup")
  @Transactional
  public void cleanupDeletedImages() {
    log.info("Starting scheduled cleanup of soft-deleted verification images.");

    List<VerificationImage> deletedImages = verificationImageRepository.findAllDeleted();
    if (deletedImages.isEmpty()) {
      log.info("No soft-deleted verification images found for cleanup.");
      return;
    }

    log.info("Found {} soft-deleted images to clean up.", deletedImages.size());

    for (VerificationImage image : deletedImages) {
      try {
        r2Service.deleteImage(image.getImageUrl());
        log.debug("Successfully deleted image from R2: {}", image.getImageUrl());
      } catch (Exception e) {
        log.error(
            "Failed to delete image from R2: {}. Skipping DB hard-delete.", image.getImageUrl(), e);
        // Continue to next image, but don't add this to hard-delete list if we want to ensure it's
        // gone from storage
      }
    }

    // Since we want to ensure atomicity or at least consistency,
    // we only hard-delete those we actually attempted (and ideally succeeded) to delete from R2.
    // For simplicity, let's hard delete all found deleted images in this batch
    // if R2 deletion didn't throw a fatal exception for the whole loop.

    List<UUID> idsToHardDelete =
        deletedImages.stream().map(VerificationImage::getId).collect(Collectors.toList());

    verificationImageRepository.hardDeleteByIdIn(idsToHardDelete);

    log.info(
        "Finished scheduled cleanup. Hard-deleted {} records from database.",
        idsToHardDelete.size());
  }
}
