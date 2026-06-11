package com.services.core.applydays.repository;

import com.services.core.applydays.entity.ApplyDaysStatistics;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApplyDaysStatisticsRepository extends JpaRepository<ApplyDaysStatistics, Long> {

  @Cacheable(value = "companySummary", key = "#companySlug")
  List<ApplyDaysStatistics> findAllByCompanySlug(String companySlug);

  @Modifying(flushAutomatically = true)
  @Query("UPDATE ApplyDaysStatistics s SET s.companySlug = :newSlug WHERE s.companySlug = :oldSlug")
  void updateCompanySlug(@Param("oldSlug") String oldSlug, @Param("newSlug") String newSlug);
}
