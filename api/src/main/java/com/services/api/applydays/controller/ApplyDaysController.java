package com.services.api.applydays.controller;

import com.services.api.applydays.dto.ApplicationRequest;
import com.services.api.applydays.dto.ApplicationViewRequest;
import com.services.api.applydays.dto.BulkDeleteRequest;
import com.services.api.applydays.dto.CommonMessageResponse;
import com.services.api.applydays.dto.CompanySummaryResponse;
import com.services.api.applydays.dto.MyApplicationResponse;
import com.services.api.applydays.dto.MyApplicationsDashboardResponse;
import com.services.api.applydays.service.ApplyDaysCommandService;
import com.services.api.applydays.service.ApplyDaysQueryService;
import com.services.core.applydays.dto.ApplicationDetailResponse;
import com.services.core.applydays.dto.CompanyListResponse;
import com.services.core.applydays.dto.MyApplicationsSummaryResponse;
import com.services.core.applydays.dto.PublicSummaryResponse;
import com.services.core.applydays.dto.TimelineBasicResponse;
import com.services.core.applydays.entity.Category;
import com.services.core.applydays.entity.VerificationStatus;
import com.services.core.common.dto.BaseResponse;
import com.services.core.common.dto.CompanyResponse;
import com.services.core.common.dto.PageResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/applydays")
@RequiredArgsConstructor
public class ApplyDaysController {

  private final ApplyDaysCommandService applyDaysCommandService;
  private final ApplyDaysQueryService applyDaysQueryService;

  @GetMapping("/companies/{slug}/timeline")
  public BaseResponse<PageResponse<? extends TimelineBasicResponse>> getCompanyTimeline(
      Authentication authentication,
      @PathVariable String slug,
      @PageableDefault(size = 10) Pageable pageable) {
    return BaseResponse.of(
        HttpStatus.OK,
        PageResponse.of(applyDaysQueryService.getCompanyTimeline(authentication, slug, pageable)));
  }

  @GetMapping("/companies/search")
  public BaseResponse<List<CompanyResponse>> searchCompanies(@RequestParam String query) {
    return BaseResponse.of(HttpStatus.OK, applyDaysQueryService.searchCompanies(query));
  }

  @GetMapping("/companies")
  public BaseResponse<PageResponse<CompanyListResponse>> getCompanies(
      Authentication authentication,
      @RequestParam(required = false) String query,
      Pageable pageable) {
    return BaseResponse.of(
        HttpStatus.OK, applyDaysQueryService.getCompanies(authentication, query, pageable));
  }

  @GetMapping("/categories")
  public BaseResponse<List<Category>> getCategories() {
    return BaseResponse.of(HttpStatus.OK, applyDaysQueryService.getCategories());
  }

  @PostMapping("/applications")
  public BaseResponse<UUID> registerApplication(
      Authentication authentication, @RequestBody ApplicationRequest request) {
    UUID appId = applyDaysCommandService.registerApplication(authentication.getName(), request);
    return BaseResponse.of(HttpStatus.CREATED, appId);
  }

  @DeleteMapping("/applications/{id}")
  public BaseResponse<Void> deleteApplication(
      Authentication authentication, @PathVariable UUID id) {
    applyDaysCommandService.deleteApplication(authentication.getName(), id);
    return BaseResponse.of(HttpStatus.OK, null);
  }

  @PostMapping("/applications/bulk-delete")
  public BaseResponse<Void> bulkDeleteApplications(
      Authentication authentication, @RequestBody BulkDeleteRequest request) {
    applyDaysCommandService.deleteApplications(authentication.getName(), request.ids());
    return BaseResponse.of(HttpStatus.OK, null);
  }

  @GetMapping("/my/dashboard")
  public BaseResponse<MyApplicationsDashboardResponse> getMyApplicationsDashboard(
      Authentication authentication, @PageableDefault(size = 10) Pageable pageable) {
    return BaseResponse.of(
        HttpStatus.OK,
        applyDaysQueryService.getMyApplicationsDashboard(authentication.getName(), pageable));
  }

  @GetMapping("/my/summary")
  public BaseResponse<MyApplicationsSummaryResponse> getMyApplicationsSummary(
      Authentication authentication) {
    return BaseResponse.of(
        HttpStatus.OK, applyDaysQueryService.getMyApplicationsSummary(authentication.getName()));
  }

  @GetMapping("/my/applications")
  public BaseResponse<PageResponse<MyApplicationResponse>> getMyApplications(
      Authentication authentication,
      @RequestParam(required = false) VerificationStatus status,
      Pageable pageable) {
    return BaseResponse.of(
        HttpStatus.OK,
        PageResponse.of(
            applyDaysQueryService.getMyApplications(authentication.getName(), status, pageable)));
  }

  @PostMapping("/applications/{id}/view")
  public BaseResponse<ApplicationDetailResponse> viewApplication(
      Authentication authentication,
      @PathVariable UUID id,
      @RequestBody ApplicationViewRequest request) {
    return BaseResponse.of(
        HttpStatus.OK,
        applyDaysQueryService.viewApplication(authentication.getName(), id, request.password()));
  }

  @GetMapping("/companies/{companySlug}")
  public BaseResponse<CompanySummaryResponse> getCompanySummary(
      Authentication authentication, @PathVariable String companySlug) {
    return BaseResponse.of(
        HttpStatus.OK, applyDaysQueryService.getCompanySummary(authentication, companySlug));
  }

  @GetMapping("/companies/{companySlug}/details")
  public BaseResponse<List<ApplicationDetailResponse>> getCompanyDetails(
      Authentication authentication, @PathVariable String companySlug) {
    return BaseResponse.of(
        HttpStatus.OK, applyDaysQueryService.getCompanyDetails(authentication, companySlug));
  }

  @GetMapping("/statistics/summary")
  public BaseResponse<PublicSummaryResponse> getPublicSummary() {
    return BaseResponse.of(HttpStatus.OK, applyDaysQueryService.getPublicSummary());
  }

  @GetMapping("/statistics/category")
  public BaseResponse<CommonMessageResponse> getCategoryStatistics(Authentication authentication) {
    return BaseResponse.of(
        HttpStatus.OK, CommonMessageResponse.of("Statistics for primary job categories"));
  }

  @GetMapping("/statistics/detail")
  public BaseResponse<CommonMessageResponse> getDetailedStatistics(Authentication authentication) {
    return BaseResponse.of(
        HttpStatus.OK,
        CommonMessageResponse.of("Detailed timeline and secondary category statistics"));
  }

  @GetMapping("/statistics/premium")
  public BaseResponse<CommonMessageResponse> getPremiumStatistics(Authentication authentication) {
    return BaseResponse.of(
        HttpStatus.OK, CommonMessageResponse.of("Premium detailed review and analytics data"));
  }
}
