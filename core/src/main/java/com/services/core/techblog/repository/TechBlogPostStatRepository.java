package com.services.core.techblog.repository;

import com.services.core.techblog.entity.TechBlogPostStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TechBlogPostStatRepository extends JpaRepository<TechBlogPostStat, Long> {

  @Modifying
  @Query("UPDATE TechBlogPostStat s SET s.clickCount = s.clickCount + 1 WHERE s.postId = :postId")
  int incrementClickCount(@Param("postId") Long postId);
}
