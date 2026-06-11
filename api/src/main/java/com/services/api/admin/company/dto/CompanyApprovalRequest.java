package com.services.api.admin.company.dto;

import com.services.core.common.persistence.entity.CompanyStatus;

public record CompanyApprovalRequest(String slug, String newSlug, CompanyStatus status) {}
