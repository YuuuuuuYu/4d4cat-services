package com.services.api.admin.company.service;

import com.services.api.admin.company.dto.CompanyApprovalRequest;
import com.services.core.applydays.repository.ApplicationRepository;
import com.services.core.applydays.repository.ApplyDaysStatisticsRepository;
import com.services.core.common.exception.BadRequestException;
import com.services.core.common.exception.ErrorCode;
import com.services.core.common.exception.NotFoundException;
import com.services.core.common.persistence.entity.Company;
import com.services.core.common.persistence.entity.CompanyStatus;
import com.services.core.common.persistence.repository.CompanyRepository;
import com.services.core.techblog.repository.TechBlogPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCompanyService {

  private final CompanyRepository companyRepository;
  private final ApplicationRepository applicationRepository;
  private final TechBlogPostRepository techBlogPostRepository;
  private final ApplyDaysStatisticsRepository applyDaysStatisticsRepository;

  public Slice<Company> getAllCompanies(Pageable pageable) {
    return companyRepository.findAll(pageable);
  }

  public Slice<Company> getSuggestedCompanies(Pageable pageable) {
    return companyRepository.findByStatus(CompanyStatus.SUGGESTED, pageable);
  }

  @Transactional
  public void approveCompany(CompanyApprovalRequest request) {
    log.info("Approving company: {} with status: {}", request.slug(), request.status());
    Company company =
        companyRepository
            .findBySlug(request.slug())
            .orElseThrow(() -> new NotFoundException(ErrorCode.COMPANY_NOT_FOUND));

    String oldSlug = request.slug();
    String newSlug = request.newSlug();

    if (StringUtils.hasText(newSlug) && !newSlug.equals(oldSlug)) {
      if (companyRepository.existsBySlug(newSlug)) {
        throw new BadRequestException(ErrorCode.DUPLICATE_COMPANY_SLUG);
      }

      log.info("Updating company slug from {} to {}", oldSlug, newSlug);

      company.updateSlug(newSlug);

      applicationRepository.updateCompanySlug(oldSlug, newSlug);
      techBlogPostRepository.updateCompanySlug(oldSlug, newSlug);
      applyDaysStatisticsRepository.updateCompanySlug(oldSlug, newSlug);
    }

    company.updateStatus(request.status());

    if (!company.getSlug().equals(company.getName())) {
      applicationRepository.updateCompanySlug(company.getName(), company.getSlug());
    }
  }

  @Transactional
  public void deleteCompany(String slug) {
    Company company =
        companyRepository
            .findBySlug(slug)
            .orElseThrow(() -> new NotFoundException(ErrorCode.COMPANY_NOT_FOUND));
    companyRepository.delete(company);
  }
}
