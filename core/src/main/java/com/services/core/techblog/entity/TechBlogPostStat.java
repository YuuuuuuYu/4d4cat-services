package com.services.core.techblog.entity;

import com.services.core.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "techblog_post_stat")
public class TechBlogPostStat extends BaseEntity {

  @Id
  @Column(name = "post_id", nullable = false)
  private Long postId;

  @Column(name = "post_title", nullable = false)
  private String postTitle;

  @Column(name = "click_count", nullable = false)
  private long clickCount = 0;

  public TechBlogPostStat(Long postId, String postTitle) {
    this.postId = postId;
    this.postTitle = postTitle;
    this.clickCount = 0;
  }
}
