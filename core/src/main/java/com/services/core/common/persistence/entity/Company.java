package com.services.core.common.persistence.entity;

import com.services.core.common.persistence.BaseSoftDeleteEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.domain.Persistable;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted = false")
@SQLDelete(sql = "UPDATE company SET deleted = true WHERE id = ?")
@Table(
    name = "company",
    indexes = {
      @Index(name = "idx_company_slug", columnList = "slug", unique = true),
      @Index(name = "idx_company_name", columnList = "name"),
      @Index(name = "idx_company_name_chosung", columnList = "name_chosung")
    })
public class Company extends BaseSoftDeleteEntity implements Persistable<UUID> {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "slug", nullable = false)
  private String slug;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "name_chosung")
  private String nameChosung;

  @Column(name = "feed_url")
  private String feedUrl;

  @PrePersist
  @PreUpdate
  public void updateChosung() {
    this.nameChosung = extractChosung(this.name);
  }

  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private CompanyStatus status = CompanyStatus.VERIFIED;

  public Company(String slug, String name, String feedUrl) {
    this.slug = slug;
    this.name = name;
    this.feedUrl = feedUrl;
    this.status = CompanyStatus.VERIFIED;
    this.nameChosung = extractChosung(name);
  }

  @Override
  public UUID getId() {
    return id;
  }

  @Override
  public boolean isNew() {
    return getCreatedAt() == null;
  }

  public void updateSlug(String slug) {
    this.slug = slug;
  }

  public void updateStatus(CompanyStatus status) {
    this.status = status;
  }

  private String extractChosung(String text) {
    if (text == null) return null;
    char[] CHOSUNG = {
      'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
    };
    StringBuilder sb = new StringBuilder();
    for (char c : text.toCharArray()) {
      if (c >= 0xAC00 && c <= 0xD7A3) {
        int index = (c - 0xAC00) / 588;
        sb.append(CHOSUNG[index]);
      } else {
        sb.append(c);
      }
    }
    return sb.toString();
  }
}
