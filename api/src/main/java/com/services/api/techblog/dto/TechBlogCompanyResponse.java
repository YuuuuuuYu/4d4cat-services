package com.services.api.techblog.dto;

import com.services.core.common.dto.CompanyResponse;

public record TechBlogCompanyResponse(String slug, String name) {
  public static TechBlogCompanyResponse from(CompanyResponse info) {
    return new TechBlogCompanyResponse(info.slug(), info.name());
  }
}
