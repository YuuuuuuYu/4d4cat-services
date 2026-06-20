package com.services.core.applydays.entity;

import com.services.core.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "verification_image")
public class VerificationImage extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "application_id", nullable = false)
  private UUID applicationId;

  @Column(name = "image_url", nullable = false)
  private String imageUrl;

  @Column(name = "original_name", nullable = false)
  private String originalName;

  @Builder
  public VerificationImage(UUID applicationId, String imageUrl, String originalName) {
    this.applicationId = applicationId;
    this.imageUrl = imageUrl;
    this.originalName = originalName;
  }
}
