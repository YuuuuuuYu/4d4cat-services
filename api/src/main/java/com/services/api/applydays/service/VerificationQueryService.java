package com.services.api.applydays.service;

import com.services.core.applydays.entity.VerificationImage;
import com.services.core.applydays.repository.VerificationImageRepository;
import com.services.core.common.exception.ErrorCode;
import com.services.core.common.exception.NotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VerificationQueryService {

  private final VerificationImageRepository verificationImageRepository;
  private final R2Service r2Service;

  public byte[] getImageBytes(UUID imageId) {
    VerificationImage image =
        verificationImageRepository
            .findById(imageId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.DATA_NOT_FOUND));
    return r2Service.getImageBytes(image.getImageUrl());
  }
}
