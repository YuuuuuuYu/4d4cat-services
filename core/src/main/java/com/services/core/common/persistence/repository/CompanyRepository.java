package com.services.core.common.persistence.repository;

import com.services.core.common.persistence.entity.Company;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CompanyRepository extends JpaRepository<Company, String> {

  @Query("SELECT c.slug as slug, c.name as name FROM Company c WHERE c.feedUrl IS NOT NULL")
  List<CompanyInfo> findAllActiveCompanies();
}
