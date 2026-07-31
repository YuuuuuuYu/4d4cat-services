package com.services.data.scheduler.applydays;

import com.services.core.common.notification.NotificationReportable;

public record SubscriptionJobReport(int renewals, int expirations)
    implements NotificationReportable {
  @Override
  public String toNotificationDescription() {
    return String.format(
        "정기 결제 및 만료 처리 결과:\n" + "• **결제 갱신 처리:** %d 건\n" + "• **만료 처리:** %d 건",
        renewals, expirations);
  }
}
