package com.services.omniwatch;

import com.services.common.application.dto.BaseEntity;
import com.services.omniwatch.attribute.Work;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WatchWork extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "watch_id")
  private Watch watch;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "work_id")
  private Work work;

  public static WatchWork createWatchWork(Watch watch, Work work) {
    WatchWork watchWork = new WatchWork();
    watchWork.watch = watch;
    watchWork.work = work;
    return watchWork;
  }
}
