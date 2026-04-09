package com.services.core.techblog.entity;

import com.services.core.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "techblog_post_tag")
public class TechBlogPostTag extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "post_id", nullable = false)
  private TechBlogPost post;

  @Column(name = "tag_name", nullable = false)
  private String tagName;

  public TechBlogPostTag(TechBlogPost post, String tagName) {
    this.post = post;
    this.tagName = tagName;
  }
}
