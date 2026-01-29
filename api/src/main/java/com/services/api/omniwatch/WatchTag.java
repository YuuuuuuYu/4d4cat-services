package com.services.api.omniwatch;

import com.services.api.omniwatch.attribute.Tag;
import com.services.api.omniwatch.dto.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WatchTag extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "watch_id")
  private Watch watch;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tag_id")
  private Tag tag;

  public static WatchTag createWatchTag(Watch watch, Tag tag) {
    WatchTag watchTag = new WatchTag();
    watchTag.watch = watch;
    watchTag.tag = tag;
    return watchTag;
  }
}
