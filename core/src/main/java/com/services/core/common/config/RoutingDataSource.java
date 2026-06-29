package com.services.core.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
public class RoutingDataSource extends AbstractRoutingDataSource {

  @Override
  protected Object determineCurrentLookupKey() {
    boolean isReadOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
    DataSourceType type = isReadOnly ? DataSourceType.READ : DataSourceType.WRITE;
    log.debug("Current Transaction isReadOnly: {}, routing to {}", isReadOnly, type);
    return type;
  }
}
