package com.services.core.common.notification;

public interface NotificationReportable {
  String toNotificationDescription();

  default boolean isWarning() {
    return false;
  }
}
