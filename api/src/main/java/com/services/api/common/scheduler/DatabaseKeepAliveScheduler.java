package com.services.api.common.scheduler;

import com.services.core.aop.NotifyDiscord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DatabaseKeepAliveScheduler {

  private static final Logger log = LoggerFactory.getLogger(DatabaseKeepAliveScheduler.class);

  private final JdbcTemplate jdbcTemplate;

  public DatabaseKeepAliveScheduler(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Scheduled(cron = "0 0 5,17 * * *")
  @NotifyDiscord(taskName = "Database Keep-alive")
  public void keepAlive() {
    jdbcTemplate.execute("SELECT 1");
    log.info("Executed keep-alive dummy query to prevent database from sleeping.");
  }
}
