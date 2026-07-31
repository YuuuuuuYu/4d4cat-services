package com.services.data.scheduler.applydays;

import com.services.core.common.notification.NotificationReportable;

public record CleanupReport(int deletedFromR2, int deletedFromDb)
    implements NotificationReportable {
  @Override
  public String toNotificationDescription() {
    return String.format(
        "이미지 정리 결과:\n" + "• **R2 삭제 수:** %d 건\n" + "• **DB hard-delete 수:** %d 건",
        deletedFromR2, deletedFromDb);
  }
}
