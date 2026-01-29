package com.services.api.omniwatch;

import com.services.api.omniwatch.dto.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WatchImage extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String originName;

  @Column(nullable = false, unique = true)
  private String storedName;

  @Column(nullable = false)
  private String storagePath;

  @Column(nullable = false)
  private String imageUrl;

  @Column(nullable = false)
  private String thumbnailUrl;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "watch_id")
  private Watch watch;

  @Builder
  public WatchImage(
      String originName,
      String storedName,
      String storagePath,
      String imageUrl,
      String thumbnailUrl,
      Watch watch) {
    this.originName = originName;
    this.storedName = storedName;
    this.storagePath = storagePath;
    this.imageUrl = imageUrl;
    this.thumbnailUrl = thumbnailUrl;
    this.watch = watch;
  }
}
