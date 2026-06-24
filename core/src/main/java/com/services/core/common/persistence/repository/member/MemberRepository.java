package com.services.core.common.persistence.repository.member;

import com.services.core.common.persistence.entity.member.Member;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, UUID> {
  Optional<Member> findByEmail(String email);

  long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
