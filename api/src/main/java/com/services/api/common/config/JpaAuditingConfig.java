package com.services.api.common.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaAuditing
@EntityScan(basePackages = {"com.services.api", "com.services.core"})
@EnableJpaRepositories(basePackages = {"com.services.api", "com.services.core"})
public class JpaAuditingConfig {}
