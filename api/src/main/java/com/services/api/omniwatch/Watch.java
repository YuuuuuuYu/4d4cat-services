package com.services.api.omniwatch;

import com.services.api.omniwatch.attribute.WatchType;
import com.services.api.omniwatch.brand.Brand;
import com.services.api.omniwatch.dto.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(indexes = @Index(name = "idx_watch_slug", columnList = "slug"))
public class Watch extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true, nullable = false)
  private String slug;

  @Column(nullable = false)
  private String koreanName;

  private String englishName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private WatchType watchType;

  private LocalDate originalDate;

  @Column(columnDefinition = "TEXT")
  private String description;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "brand_id")
  private Brand brand;

  @Column(length = 1000)
  private String referenceUrl;

  @OneToOne(mappedBy = "watch", cascade = CascadeType.ALL, orphanRemoval = true)
  private WatchImage watchImage;

  @OneToMany(mappedBy = "watch", cascade = CascadeType.ALL)
  private List<WatchTag> watchTags = new ArrayList<>();

  @OneToMany(mappedBy = "watch", cascade = CascadeType.ALL)
  private List<WatchMaterial> watchMaterials = new ArrayList<>();

  @OneToMany(mappedBy = "watch", cascade = CascadeType.ALL)
  private List<WatchWork> watchWorks = new ArrayList<>();

  @Builder
  public Watch(
      String koreanName,
      String englishName,
      String slug,
      WatchType watchType,
      LocalDate originalDate,
      String description,
      Brand brand,
      String referenceUrl) {
    this.koreanName = koreanName;
    this.englishName = englishName;
    this.slug = slug;
    this.watchType = watchType;
    this.originalDate = originalDate;
    this.description = description;
    this.brand = brand;
    this.referenceUrl = referenceUrl;
  }

  public void updateBasicInfo(String koreanName, String description) {
    this.koreanName = koreanName;
    this.description = description;
  }
}
