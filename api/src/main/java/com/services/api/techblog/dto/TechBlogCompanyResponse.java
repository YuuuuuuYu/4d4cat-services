package com.services.api.techblog.dto;

import com.services.core.techblog.repository.TechBlogCompanyInfo;

public record TechBlogCompanyResponse(String slug, String name) {
  public static TechBlogCompanyResponse from(TechBlogCompanyInfo info) {
    return new TechBlogCompanyResponse(info.getSlug(), info.getName());
  }
}
