package com.services.core.common.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

@Getter
@MappedSuperclass
public abstract class BaseSoftDeleteEntity extends BaseEntity {

  @Column(nullable = false)
  private boolean deleted = false;

  public void delete() {
    this.deleted = true;
  }

  public void restore() {
    this.deleted = false;
  }
}
