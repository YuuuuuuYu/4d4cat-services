package com.services.api.techblog.dto;

import java.util.List;

public record TechBlogListResponse(
    List<TechBlogResponse> items, Long nextCursor, boolean hasNext) {}
