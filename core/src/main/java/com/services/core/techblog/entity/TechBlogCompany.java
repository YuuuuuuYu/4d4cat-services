package com.services.core.techblog.entity;

import com.services.core.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "techblog_company")
public class TechBlogCompany extends BaseEntity implements Persistable<String> {

  @Id
  @Column(name = "slug", nullable = false, unique = true)
  private String slug;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "feed_url", nullable = false)
  private String feedUrl;

  public TechBlogCompany(String slug, String name, String feedUrl) {
    this.slug = slug;
    this.name = name;
    this.feedUrl = feedUrl;
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
