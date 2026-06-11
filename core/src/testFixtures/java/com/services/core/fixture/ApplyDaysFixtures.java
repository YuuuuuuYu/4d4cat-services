package com.services.core.fixture;

import com.services.core.applydays.dto.HiringStepDetail;
import com.services.core.applydays.entity.Application;
import com.services.core.applydays.entity.ApplicationChannel;
import com.services.core.common.persistence.entity.Company;
import com.services.core.common.persistence.entity.member.Member;
import com.services.core.common.persistence.entity.member.Role;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.test.util.ReflectionTestUtils;

public class ApplyDaysFixtures {
  public static Company createCompany() {
    return Company.builder().slug("naver").name("Naver").feedUrl("https://naver.com/rss").build();
  }

  public static Application createApplication(String companySlug, Long categoryId) {
    return Application.builder()
        .id(UUID.randomUUID())
        .companySlug(companySlug)
        .categoryId(categoryId)
        .positionDetail("Software Engineer")
        .appliedAt(LocalDateTime.now())
        .hiringProcess(
            List.of(
                HiringStepDetail.builder()
                    .stepType("DOCUMENT")
                    .stepDate("2025-01-01")
                    .status("PASSED")
                    .durationDays(3)
                    .build()))
        .channel(ApplicationChannel.DIRECT)
        .build();
  }

  public static Member createMember(String email, Role role) {
    return Member.builder()
        .email(email)
        .name("Test User")
        .providerId("google-123")
        .role(role)
        .build();
  }

  public static <T> void setId(T entity, Object id) {
    ReflectionTestUtils.setField(entity, "id", id);
  }
}
