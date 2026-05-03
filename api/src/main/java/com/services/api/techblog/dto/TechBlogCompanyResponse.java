package com.services.api.techblog.dto;

import com.services.core.common.persistence.repository.CompanyInfo;

public record TechBlogCompanyResponse(String slug, String name) {
  public static TechBlogCompanyResponse from(CompanyInfo info) {
    return new TechBlogCompanyResponse(info.getSlug(), info.getName());
  }
}
