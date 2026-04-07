package com.services.api.techblog.dto;

import com.services.core.techblog.entity.TechBlogPost;
import com.services.core.techblog.entity.TechBlogPostTag;
import java.time.LocalDateTime;
import java.util.List;

public record TechBlogResponse(
    Long id,
    String companyName,
    String companySlug,
    String title,
    String url,
    LocalDateTime publishedAt,
    List<String> tags) {

  public static TechBlogResponse from(TechBlogPost post) {
    return new TechBlogResponse(
        post.getId(),
        post.getCompany().getName(),
        post.getCompany().getSlug(),
        post.getTitle(),
        post.getUrl(),
        post.getPublishedAt(),
        post.getTags().stream().map(TechBlogPostTag::getTagName).toList());
  }
}
