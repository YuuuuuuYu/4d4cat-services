package com.services.api.pixabay;

import com.services.core.exception.ErrorCode;
import com.services.core.infrastructure.ApiMetadata;
import com.services.core.infrastructure.RedisDataStorage;
import com.services.core.pixabay.dto.PixabayMusicResult;
import com.services.core.pixabay.dto.PixabayVideoResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PixabayService {

  private final RedisDataStorage redisDataStorage;

  public PixabayVideoResult getRandomVideo() {
    return redisDataStorage.getRandomElement(
        ApiMetadata.PIXABAY_VIDEOS.getKey(),
        PixabayVideoResult.class,
        ErrorCode.PIXABAY_VIDEO_NOT_FOUND);
  }

  public PixabayMusicResult getRandomMusic() {
    return redisDataStorage.getRandomElement(
        ApiMetadata.PIXABAY_MUSIC.getKey(),
        PixabayMusicResult.class,
        ErrorCode.PIXABAY_MUSIC_NOT_FOUND);
  }
}
