package com.services.common.infrastructure;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ApiMetadata {
  PIXABAY_MUSIC("pixabayMusic", "pixabayMusic", "pixabay.url.music"),
  PIXABAY_VIDEOS("pixabayVideos", "pixabayVideo", "pixabay.url.video"),
  ;

  private final String key;
  private final String attributeName;
  private final String urlPropertyKey;
}
