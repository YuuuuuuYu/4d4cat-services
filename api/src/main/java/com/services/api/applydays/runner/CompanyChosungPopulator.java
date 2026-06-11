package com.services.api.applydays.runner;

import com.services.core.common.persistence.entity.Company;
import com.services.core.common.persistence.repository.CompanyRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompanyChosungPopulator implements ApplicationRunner {

  private final CompanyRepository companyRepository;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    log.info("Checking companies with missing chosung values...");
    List<Company> companiesWithMissingChosung = companyRepository.findByNameChosungIsNull();

    if (companiesWithMissingChosung.isEmpty()) {
      log.info("No companies with missing chosung found.");
      return;
    }

    log.info(
        "Found {} companies with missing chosung. Populating...",
        companiesWithMissingChosung.size());
    for (Company company : companiesWithMissingChosung) {
      company.updateChosung();
    }
    companyRepository.saveAll(companiesWithMissingChosung);
    log.info(
        "Successfully populated chosung values for {} companies.",
        companiesWithMissingChosung.size());
  }
}
