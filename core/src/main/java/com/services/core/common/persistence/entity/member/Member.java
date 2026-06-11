package com.services.core.common.persistence.entity.member;

import com.services.core.common.persistence.BaseSoftDeleteEntity;
import com.services.core.common.persistence.converter.CryptoConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted = false")
@SQLDelete(
    sql =
        "UPDATE member SET deleted = true, email = NULL, name = NULL, provider_id = NULL WHERE id = ?")
@Table(name = "member")
public class Member extends BaseSoftDeleteEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Convert(converter = CryptoConverter.class)
  @Column(unique = true)
  private String email;

  @Convert(converter = CryptoConverter.class)
  @Column
  private String name;

  @Column(name = "provider_id")
  private String providerId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Role role;

  @Builder
  public Member(String email, String name, String providerId, Role role) {
    this.email = email;
    this.name = name;
    this.providerId = providerId;
    this.role = role;
  }

  public Member update(String name) {
    this.name = name;
    return this;
  }

  public void anonymize() {
    this.email = null;
    this.name = null;
    this.providerId = null;
  }

  public void promoteToReviewer() {
    this.role = Role.REVIEWER;
  }
}
