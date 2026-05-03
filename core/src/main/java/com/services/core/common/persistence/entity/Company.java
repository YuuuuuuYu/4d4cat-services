package com.services.core.common.persistence.entity;

import com.services.core.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.domain.Persistable;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted = false")
@SQLDelete(sql = "UPDATE techblog_company SET deleted = true WHERE slug = ?")
@Table(name = "techblog_company")
public class Company extends BaseEntity implements Persistable<String> {

  @Id
  @Column(name = "slug", nullable = false, unique = true)
  private String slug;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "feed_url", nullable = false)
  private String feedUrl;

  @Column(name = "brn", unique = true)
  private String brn;

  public Company(String slug, String name, String feedUrl) {
    this.slug = slug;
    this.name = name;
    this.feedUrl = feedUrl;
  }

  public Company(String slug, String name, String feedUrl, String brn) {
    this.slug = slug;
    this.name = name;
    this.feedUrl = feedUrl;
    this.brn = brn;
  }

  @Override
  public String getId() {
    return slug;
  }

  @Override
  public boolean isNew() {
    return getCreatedAt() == null;
  }
}
