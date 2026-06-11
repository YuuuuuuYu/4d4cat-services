package com.services.api.admin.category.dto;

public record CategoryCreateRequest(String name, Long parentId, Integer depth) {}
