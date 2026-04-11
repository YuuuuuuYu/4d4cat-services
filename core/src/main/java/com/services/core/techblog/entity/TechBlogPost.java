package com.services.core.techblog.entity;

import com.services.core.common.persistence.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "techblog_post")
public class TechBlogPost extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "company_slug", nullable = false)
  private TechBlogCompany company;

  @Column(name = "title", nullable = false, length = 1024)
  private String title;

  @Column(name = "url", nullable = false, unique = true, length = 1024)
  private String url;

  @Column(name = "published_at")
  private LocalDateTime publishedAt;

  @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<TechBlogPostTag> tags = new ArrayList<>();

  public TechBlogPost(
      TechBlogCompany company, String title, String url, LocalDateTime publishedAt) {
    this.company = company;
    this.title = title;
    this.url = url;
    this.publishedAt = publishedAt;
  }

  public void addTag(TechBlogPostTag tag) {
    this.tags.add(tag);
  }
}
