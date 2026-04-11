package com.services.core.techblog.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.services.core.fixture.TechBlogFixtures;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TechBlogCompanyTest {

  @Test
  @DisplayName("create - 새 회사 객체 생성 시 Auditing 필드는 null")
  void create_shouldHaveNullAuditingFields() {
    // When
    TechBlogCompany company = TechBlogFixtures.createDefaultCompany();

    // Then
    assertThat(company.getCreatedAt()).isNull();
    assertThat(company.getUpdatedAt()).isNull();
    assertThat(company.isNew()).isTrue();
  }

  @Test
  @DisplayName("setAuditingFields - 호출 시 createdAt과 updatedAt이 현재 시간으로 설정됨")
  void setAuditingFields_shouldPopulateDates() {
    // Given
    TechBlogCompany company = TechBlogFixtures.createDefaultCompany();

    // When
    TechBlogFixtures.setAuditingFields(company);

    // Then
    assertThat(company.getCreatedAt()).isNotNull();
    assertThat(company.getUpdatedAt()).isNotNull();
    assertThat(company.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    assertThat(company.isNew()).isFalse();
  }

  @Test
  @DisplayName("dateLogic - Auditing 필드가 채워지면 날짜 기반 로직 정상 동작")
  void dateLogic_shouldWorkWithoutNPE() {
    // Given
    TechBlogCompany company = TechBlogFixtures.createDefaultCompany();
    TechBlogFixtures.setAuditingFields(company);

    // When & Then
    assertThatCode(() -> {
      LocalDateTime now = LocalDateTime.now();
      boolean isCreatedToday = company.getCreatedAt().isAfter(now.minusDays(1));
      assertThat(isCreatedToday).isTrue();
    }).doesNotThrowAnyException();
  }
}
