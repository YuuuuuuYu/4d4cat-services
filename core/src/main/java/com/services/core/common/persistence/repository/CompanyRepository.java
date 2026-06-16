package com.services.core.common.persistence.repository;

import com.services.core.common.persistence.entity.Company;
import com.services.core.common.persistence.entity.CompanyStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, UUID>, CompanyRepositoryCustom {

  Optional<Company> findByName(String name);

  Optional<Company> findBySlug(String slug);

  List<Company> findBySlugIn(Collection<String> slugs);

  boolean existsBySlug(String slug);

  Slice<Company> findByStatus(CompanyStatus status, Pageable pageable);

  List<Company> findByNameChosungIsNull();

  List<Company> findByFeedUrlIsNotNull();
}
