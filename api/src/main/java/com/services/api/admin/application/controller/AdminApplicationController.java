package com.services.api.admin.application.controller;

import com.services.api.admin.application.service.AdminApplicationService;
import com.services.api.applydays.dto.RejectionRequest;
import com.services.api.applydays.dto.VerificationImageResponse;
import com.services.core.applydays.dto.AdminPendingRequestResponse;
import com.services.core.applydays.entity.Application;
import com.services.core.applydays.repository.ApplicationRepository;
import com.services.core.applydays.repository.VerificationImageRepository;
import com.services.core.common.dto.BaseResponse;
import com.services.core.common.exception.ErrorCode;
import com.services.core.common.exception.NotFoundException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/applications")
@RequiredArgsConstructor
public class AdminApplicationController {

  private final AdminApplicationService adminApplicationService;
  private final ApplicationRepository applicationRepository;
  private final VerificationImageRepository verificationImageRepository;

  @GetMapping("/requests/pending")
  public BaseResponse<Slice<AdminPendingRequestResponse>> getPendingRequests(Pageable pageable) {
    return BaseResponse.of(HttpStatus.OK, adminApplicationService.getPendingRequests(pageable));
  }

  @GetMapping("/{id}")
  public BaseResponse<Application> getApplication(@PathVariable UUID id) {
    return BaseResponse.of(
        HttpStatus.OK,
        applicationRepository
            .findById(id)
            .orElseThrow(() -> new NotFoundException(ErrorCode.APPLICATION_NOT_FOUND)));
  }

  @GetMapping("/{id}/images")
  public BaseResponse<List<VerificationImageResponse>> getApplicationImages(@PathVariable UUID id) {
    List<VerificationImageResponse> responses =
        verificationImageRepository.findAllByApplicationId(id).stream()
            .map(VerificationImageResponse::from)
            .toList();
    return BaseResponse.of(HttpStatus.OK, responses);
  }

  @GetMapping(
      value = "/verification/images/{imageId}",
      produces = {MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE, "image/webp"})
  public ResponseEntity<byte[]> getVerificationImage(@PathVariable UUID imageId) {
    return ResponseEntity.ok(adminApplicationService.getImageBytes(imageId));
  }

  @PostMapping("/requests/{id}/approve")
  public BaseResponse<Void> approveRequest(@PathVariable UUID id) {
    adminApplicationService.approveRequest(id);
    return BaseResponse.of(HttpStatus.OK, null);
  }

  @PostMapping("/requests/{id}/reject")
  public BaseResponse<Void> rejectRequest(
      @PathVariable UUID id, @RequestBody RejectionRequest request) {
    adminApplicationService.rejectRequest(id, request.reason());
    return BaseResponse.of(HttpStatus.OK, null);
  }
}
