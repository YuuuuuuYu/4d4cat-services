package com.services.api.applydays.controller;

import com.services.api.applydays.dto.ApplicationRequest;
import com.services.api.applydays.dto.ApprovalRequest;
import com.services.api.applydays.dto.BulkApproveRequest;
import com.services.api.applydays.dto.BulkRejectRequest;
import com.services.api.applydays.dto.RejectionRequest;
import com.services.api.applydays.dto.VerificationImageResponse;
import com.services.api.applydays.service.AdminApplyDaysCommandService;
import com.services.api.applydays.service.AdminApplyDaysQueryService;
import com.services.api.common.infrastructure.external.redis.RedisMessagePublisher;
import com.services.core.applydays.dto.AdminApplicationDetailResponse;
import com.services.core.applydays.dto.AdminApplicationResponse;
import com.services.core.applydays.dto.AdminPendingRequestResponse;
import com.services.core.applydays.repository.VerificationImageRepository;
import com.services.core.common.dto.BaseResponse;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/applydays")
@RequiredArgsConstructor
public class AdminApplyDaysController {

  private final AdminApplyDaysCommandService adminApplyDaysCommandService;
  private final AdminApplyDaysQueryService adminApplyDaysQueryService;
  private final VerificationImageRepository verificationImageRepository;
  private final RedisMessagePublisher redisMessagePublisher;

  @GetMapping("/notifications/pending-count")
  public BaseResponse<Integer> getPendingNotificationCount() {
    return BaseResponse.of(HttpStatus.OK, adminApplyDaysQueryService.getPendingNotificationCount());
  }

  @PostMapping("/notifications/trigger-batch")
  public BaseResponse<Void> triggerNotificationBatch() {
    redisMessagePublisher.publishNotificationBatchTrigger();
    return BaseResponse.of(HttpStatus.OK, null);
  }

  @GetMapping("/requests/pending")
  public BaseResponse<Slice<AdminPendingRequestResponse>> getPendingRequests(Pageable pageable) {
    return BaseResponse.of(HttpStatus.OK, adminApplyDaysQueryService.getPendingRequests(pageable));
  }

  @GetMapping("/applications")
  public BaseResponse<Slice<AdminApplicationResponse>> getAllApplications(
      @RequestParam(required = false) String companyName,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime appliedAt,
      Pageable pageable) {
    return BaseResponse.of(
        HttpStatus.OK,
        adminApplyDaysQueryService.getAllApplications(companyName, appliedAt, pageable));
  }

  @GetMapping("/applications/{id}")
  public BaseResponse<AdminApplicationDetailResponse> getApplication(@PathVariable UUID id) {
    return BaseResponse.of(HttpStatus.OK, adminApplyDaysQueryService.getApplicationDetail(id));
  }

  @PutMapping("/applications/{id}")
  public BaseResponse<Void> updateApplication(
      @PathVariable UUID id, @Valid @RequestBody ApplicationRequest request) {
    adminApplyDaysCommandService.updateApplication(id, request);
    return BaseResponse.of(HttpStatus.OK, null);
  }

  @GetMapping("/applications/{id}/images")
  public BaseResponse<List<VerificationImageResponse>> getApplicationImages(@PathVariable UUID id) {
    List<VerificationImageResponse> responses =
        verificationImageRepository.findAllByApplicationId(id).stream()
            .map(VerificationImageResponse::from)
            .toList();
    return BaseResponse.of(HttpStatus.OK, responses);
  }

  @PostMapping("/requests/{id}/approve")
  public BaseResponse<Void> approveRequest(
      @PathVariable UUID id, @RequestBody(required = false) ApprovalRequest request) {
    String newSlug = (request != null) ? request.newSlug() : null;
    Instant scheduledAt = (request != null) ? request.scheduledAt() : null;
    adminApplyDaysCommandService.asyncApproveRequest(id, newSlug, scheduledAt);
    return BaseResponse.of(HttpStatus.ACCEPTED, null);
  }

  @PostMapping("/requests/{id}/reject")
  public BaseResponse<Void> rejectRequest(
      @PathVariable UUID id, @RequestBody RejectionRequest request) {
    adminApplyDaysCommandService.asyncRejectRequest(id, request.reason());
    return BaseResponse.of(HttpStatus.ACCEPTED, null);
  }

  @DeleteMapping("/applications/{id}")
  public BaseResponse<Void> deleteApplication(@PathVariable UUID id) {
    adminApplyDaysCommandService.deleteApplication(id);
    return BaseResponse.of(HttpStatus.OK, null);
  }

  @PostMapping("/requests/bulk-approve")
  public BaseResponse<Void> bulkApproveRequest(@RequestBody BulkApproveRequest request) {
    adminApplyDaysCommandService.bulkApproveRequest(
        request.requestIds(), request.newSlug(), request.scheduledAt());
    return BaseResponse.of(HttpStatus.ACCEPTED, null);
  }

  @PostMapping("/requests/bulk-reject")
  public BaseResponse<Void> bulkRejectRequest(@RequestBody BulkRejectRequest request) {
    adminApplyDaysCommandService.bulkRejectRequest(
        request.requestIds(), request.reason(), request.details());
    return BaseResponse.of(HttpStatus.ACCEPTED, null);
  }
}
