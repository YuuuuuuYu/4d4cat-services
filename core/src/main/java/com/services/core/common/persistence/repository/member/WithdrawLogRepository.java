package com.services.core.common.persistence.repository.member;

import com.services.core.common.persistence.entity.member.WithdrawLog;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WithdrawLogRepository extends JpaRepository<WithdrawLog, UUID> {}
