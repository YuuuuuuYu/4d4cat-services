package com.services.api.admin.company.controller;

import com.services.api.admin.company.dto.CompanyApprovalRequest;
import com.services.api.admin.company.service.AdminCompanyService;
import com.services.core.common.dto.BaseResponse;
import com.services.core.common.persistence.entity.Company;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/companies")
@RequiredArgsConstructor
public class AdminCompanyController {

  private final AdminCompanyService adminCompanyService;

  @GetMapping
  public BaseResponse<Slice<Company>> getAllCompanies(Pageable pageable) {
    return BaseResponse.of(HttpStatus.OK, adminCompanyService.getAllCompanies(pageable));
  }

  @GetMapping("/suggested")
  public BaseResponse<Slice<Company>> getSuggestedCompanies(Pageable pageable) {
    return BaseResponse.of(HttpStatus.OK, adminCompanyService.getSuggestedCompanies(pageable));
  }

  /** 회사 상태 승인 API */
  @PostMapping("/approve")
  public BaseResponse<Void> approveCompany(@RequestBody CompanyApprovalRequest request) {
    adminCompanyService.approveCompany(request);
    return BaseResponse.of(HttpStatus.OK, null);
  }

  @DeleteMapping("/{slug}")
  public BaseResponse<Void> deleteCompany(@PathVariable String slug) {
    adminCompanyService.deleteCompany(slug);
    return BaseResponse.of(HttpStatus.OK, null);
  }
}
