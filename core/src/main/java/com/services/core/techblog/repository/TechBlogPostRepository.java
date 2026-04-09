package com.services.core.techblog.repository;

import com.services.core.techblog.entity.TechBlogPost;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TechBlogPostRepository extends JpaRepository<TechBlogPost, Long> {

  @Modifying
  @Query("DELETE FROM TechBlogPost p WHERE p.deleted = true")
  void deletePhysicallyAllDeleted();

  @Modifying
  @Query(
      "UPDATE TechBlogPost p SET p.deleted = true WHERE p.id NOT IN :activeIds AND p.company.slug = :companySlug")
  void deleteMissingPosts(
      @Param("activeIds") List<Long> activeIds, @Param("companySlug") String companySlug);

  Optional<TechBlogPost> findByUrl(String url);
}
