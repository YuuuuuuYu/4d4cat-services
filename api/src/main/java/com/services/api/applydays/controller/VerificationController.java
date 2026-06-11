package com.services.api.applydays.controller;

import com.services.api.applydays.dto.PresignedUrlResponse;
import com.services.api.applydays.service.VerificationCommandService;
import com.services.api.applydays.service.VerificationQueryService;
import com.services.core.common.dto.BaseResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/applydays/verification")
@RequiredArgsConstructor
public class VerificationController {

  private final VerificationCommandService verificationCommandService;
  private final VerificationQueryService verificationQueryService;

  @PostMapping("/applications/{applicationId}/images/presigned-url")
  public BaseResponse<PresignedUrlResponse> getPresignedUrl(
      Authentication authentication,
      @PathVariable UUID applicationId,
      @RequestParam("fileName") String fileName,
      @RequestParam("contentType") String contentType) {

    PresignedUrlResponse response =
        verificationCommandService.getPresignedUrl(
            authentication.getName(), applicationId, fileName, contentType);
    return BaseResponse.of(HttpStatus.OK, response);
  }

  @PostMapping("/applications/{applicationId}/images")
  public BaseResponse<UUID> uploadImage(
      Authentication authentication,
      @PathVariable UUID applicationId,
      @RequestParam("file") MultipartFile file) {

    UUID imageId =
        verificationCommandService.uploadVerificationImage(
            authentication.getName(), applicationId, file);
    return BaseResponse.of(HttpStatus.CREATED, imageId);
  }

  @GetMapping(
      value = "/images/{imageId}",
      produces = {MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE, "image/webp"})
  public ResponseEntity<byte[]> getImage(@PathVariable UUID imageId) {
    byte[] imageBytes = verificationQueryService.getImageBytes(imageId);
    return ResponseEntity.ok(imageBytes);
  }
}
