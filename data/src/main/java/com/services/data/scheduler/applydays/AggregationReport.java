package com.services.data.scheduler.applydays;

import com.services.core.common.notification.NotificationReportable;

public record AggregationReport(long writeCount) implements NotificationReportable {
  @Override
  public String toNotificationDescription() {
    return String.format("통계 집계 작업 결과:\n" + "• **집계된 데이터 수:** %d 건", writeCount);
  }
}
