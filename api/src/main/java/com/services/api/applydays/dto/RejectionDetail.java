package com.services.api.applydays.dto;

import java.util.UUID;

public record RejectionDetail(UUID requestId, String reason) {}
