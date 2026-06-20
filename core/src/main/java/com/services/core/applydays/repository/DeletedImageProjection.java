package com.services.core.applydays.repository;

import java.util.UUID;

public interface DeletedImageProjection {
  UUID getId();

  String getImageUrl();
}
