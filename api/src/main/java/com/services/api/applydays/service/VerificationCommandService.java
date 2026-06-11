package com.services.api.applydays.service;

import com.services.api.applydays.dto.PresignedUrlResponse;
import com.services.core.applydays.entity.VerificationImage;
import com.services.core.applydays.repository.ApplicationRepository;
import com.services.core.applydays.repository.VerificationImageRepository;
import com.services.core.applydays.repository.VerificationRequestRepository;
import com.services.core.common.exception.BadRequestException;
import com.services.core.common.exception.ErrorCode;
import com.services.core.common.exception.NotFoundException;
import com.services.core.common.persistence.entity.member.Member;
import com.services.core.common.persistence.repository.member.MemberRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
public class VerificationCommandService {

  private static final int MAX_IMAGE_COUNT = 10;

  private final ApplicationRepository applicationRepository;
  private final VerificationImageRepository verificationImageRepository;
  private final VerificationRequestRepository verificationRequestRepository;
  private final MemberRepository memberRepository;
  private final R2Service r2Service;
  private final MeterRegistry meterRegistry;

  public PresignedUrlResponse getPresignedUrl(
      String email, UUID applicationId, String fileName, String contentType) {
    applicationRepository
        .findById(applicationId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.APPLICATION_NOT_FOUND));

    Member member =
        memberRepository
            .findByEmail(email)
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

    verificationRequestRepository
        .findByApplicationId(applicationId)
        .filter(vr -> vr.getMemberId().equals(member.getId()))
        .orElseThrow(() -> new BadRequestException(ErrorCode.UNAUTHORIZED_APPLICATION_ACCESS));

    long count = verificationImageRepository.countByApplicationId(applicationId);
    if (count >= MAX_IMAGE_COUNT) {
      throw new BadRequestException(ErrorCode.VERIFICATION_IMAGE_LIMIT_EXCEEDED);
    }

    String key = r2Service.generateKey(applicationId, (int) count + 1);
    String presignedUrl = r2Service.generatePresignedUrl(key, contentType);

    VerificationImage verificationImage =
        VerificationImage.builder()
            .applicationId(applicationId)
            .imageUrl(key)
            .originalName(fileName)
            .build();

    VerificationImage saved = verificationImageRepository.save(verificationImage);

    meterRegistry.counter("applydays.verification.presigned_url.issued").increment();

    return PresignedUrlResponse.builder()
        .presignedUrl(presignedUrl)
        .key(key)
        .imageId(saved.getId())
        .build();
  }

  public UUID uploadVerificationImage(String email, UUID applicationId, MultipartFile file) {
    applicationRepository
        .findById(applicationId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.APPLICATION_NOT_FOUND));

    Member member =
        memberRepository
            .findByEmail(email)
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

    verificationRequestRepository
        .findByApplicationId(applicationId)
        .filter(vr -> vr.getMemberId().equals(member.getId()))
        .orElseThrow(() -> new BadRequestException(ErrorCode.UNAUTHORIZED_APPLICATION_ACCESS));

    long count = verificationImageRepository.countByApplicationId(applicationId);
    if (count >= MAX_IMAGE_COUNT) {
      throw new BadRequestException(ErrorCode.VERIFICATION_IMAGE_LIMIT_EXCEEDED);
    }

    String imageUrl = r2Service.uploadImage(applicationId, (int) count + 1, file);

    VerificationImage verificationImage =
        VerificationImage.builder()
            .applicationId(applicationId)
            .imageUrl(imageUrl)
            .originalName(file.getOriginalFilename())
            .build();

    VerificationImage saved = verificationImageRepository.save(verificationImage);

    meterRegistry.counter("applydays.verification.images.uploaded").increment();

    return saved.getId();
  }
}
