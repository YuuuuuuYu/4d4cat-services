package com.services.core.applydays.repository;

import com.services.core.applydays.dto.AdminApplicationDetailResponse;
import com.services.core.applydays.dto.AdminApplicationResponse;
import com.services.core.applydays.dto.TimelineBasicResponse;
import com.services.core.applydays.dto.TimelineDetailResponse;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface ApplicationRepositoryCustom {
  Slice<AdminApplicationResponse> findAllWithDetails(
      String companyName, LocalDateTime appliedAt, Pageable pageable);

  Optional<AdminApplicationDetailResponse> findApplicationDetailById(UUID id);

  Slice<TimelineBasicResponse> findTimelineBasicByCompanySlug(
      String companySlug, Pageable pageable);

  Slice<TimelineDetailResponse> findTimelineDetailByCompanySlug(
      String companySlug, Pageable pageable);
}
