package com.services.core.techblog.repository;

import com.services.core.techblog.entity.TechBlogCompany;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TechBlogCompanyRepository extends JpaRepository<TechBlogCompany, String> {
  Optional<TechBlogCompany> findBySlug(String slug);
}
