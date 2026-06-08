package com.services.core.common.persistence.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.services.core.fixture.TechBlogFixtures;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CompanyTest {

  @Test
  @DisplayName("create - 새 회사 객체 생성 시 Auditing 필드는 null")
  void create_shouldHaveNullAuditingFields() {
    // When
    Company company = TechBlogFixtures.createDefaultCompany();

    // Then
    assertThat(company.getCreatedAt()).isNull();
    assertThat(company.getUpdatedAt()).isNull();
    assertThat(company.isNew()).isTrue();
  }

  @Test
  @DisplayName("setAuditingFields - 호출 시 createdAt과 updatedAt이 현재 시간으로 설정됨")
  void setAuditingFields_shouldPopulateDates() {
    // Given
    Company company = TechBlogFixtures.createDefaultCompany();

    // When
    TechBlogFixtures.setAuditingFields(company);

    // Then
    assertThat(company.getCreatedAt()).isNotNull();
    assertThat(company.getUpdatedAt()).isNotNull();
    assertThat(company.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    assertThat(company.isNew()).isFalse();
  }

  @Test
  @DisplayName("extractChosung - 한국어 기업명에서 초성이 올바르게 추출됨")
  void extractChosung_shouldWorkCorrectly() {
    // Given
    Company company = Company.builder().slug("samsung").name("삼성전자").build();

    // When
    company.updateChosung();

    // Then
    assertThat(company.getNameChosung()).isEqualTo("ㅅㅅㅈㅈ");
  }

  @Test
  @DisplayName("extractChosung - 영문 및 숫자가 섞인 경우에도 올바르게 동작함")
  void extractChosung_withMixedCharacters_shouldWorkCorrectly() {
    // Given
    Company company = Company.builder().slug("kakao").name("카카오123Bank").build();

    // When
    company.updateChosung();

    // Then
    assertThat(company.getNameChosung()).isEqualTo("ㅋㅋㅇ123Bank");
  }
}
