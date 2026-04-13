package com.services.core.techblog.repository;

import com.services.core.techblog.entity.TechBlogCompany;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TechBlogCompanyRepository extends JpaRepository<TechBlogCompany, String> {

  @Query("SELECT c.slug FROM TechBlogCompany c WHERE c.deleted = false")
  List<String> findAllActiveSlugs();
}
