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
@Table(name = "techblog_post_stat")
public class TechBlogPostStat extends BaseEntity implements Persistable<Long> {

  @Id
  @Column(name = "post_id", nullable = false)
  private Long postId;

  @Column(name = "post_title", nullable = false, length = 1024)
  private String postTitle;

  @Column(name = "click_count", nullable = false)
  private long clickCount = 0;

  public TechBlogPostStat(Long postId, String postTitle) {
    this.postId = postId;
    this.postTitle = postTitle;
    this.clickCount = 0;
  }

  @Override
  public Long getId() {
    return postId;
  }

  @Override
  public boolean isNew() {
    return getCreatedAt() == null;
  }
}
