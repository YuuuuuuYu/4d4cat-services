package com.services.core.applydays.repository;

import com.services.core.applydays.entity.NotificationQueue;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationQueueRepository extends JpaRepository<NotificationQueue, UUID> {
  List<NotificationQueue> findAllByStatus(String status);

  int countByStatus(String status);
}
