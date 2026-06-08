package com.services.core.common.persistence.repository;

import com.services.core.applydays.dto.CompanyListResponse;
import com.services.core.common.dto.CompanyResponse;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface CompanyRepositoryCustom {
  List<CompanyResponse> findAllActiveCompanies();

  List<CompanyResponse> searchByNameOrChosung(String query);

  Slice<CompanyListResponse> findAllVerifiedWithStats(String query, Pageable pageable);
}
