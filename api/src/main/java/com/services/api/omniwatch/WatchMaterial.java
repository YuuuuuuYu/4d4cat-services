package com.services.api.omniwatch;

import com.services.api.omniwatch.attribute.Material;
import com.services.api.omniwatch.dto.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WatchMaterial extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "watch_id")
  private Watch watch;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "material_id")
  private Material material;

  public static WatchMaterial createWatchMaterial(Watch watch, Material material) {
    WatchMaterial watchMaterial = new WatchMaterial();
    watchMaterial.watch = watch;
    watchMaterial.material = material;
    return watchMaterial;
  }
}
