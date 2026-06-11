package com.services.api.applydays.dto;

import com.services.core.applydays.entity.VerificationImage;
import java.util.UUID;
import lombok.Builder;

@Builder
public record VerificationImageResponse(
    UUID id, UUID applicationId, String imageUrl, String originalName) {
  public static VerificationImageResponse from(VerificationImage entity) {
    return VerificationImageResponse.builder()
        .id(entity.getId())
        .applicationId(entity.getApplicationId())
        .imageUrl("/applydays/verification/images/" + entity.getId())
        .originalName(entity.getOriginalName())
        .build();
  }
}
