package com.services.api.applydays.dto;

import java.util.UUID;
import lombok.Builder;

@Builder
public record PresignedUrlResponse(String presignedUrl, String key, UUID imageId) {}
