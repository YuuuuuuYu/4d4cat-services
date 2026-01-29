package com.services.data.pixabay;

import com.services.core.infrastructure.ApiMetadata;
import com.services.core.infrastructure.RedisDataStorage;
import com.services.core.pixabay.dto.CustomPixabayMusicResponse;
import com.services.core.pixabay.dto.PixabayMusicResult;
import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class PixabayMusicCollector
    extends PixabayDataCollector<
        PixabayMusicResult, CustomPixabayMusicResponse<PixabayMusicResult>> {

  private static final List<String> PIXABAY_MUSIC_GENRES =
      List.of(
          "electronic",
          "upbeat",
          "beats",
          "beautiful%20plays",
          "main%20title",
          "alternative%20hip%20hop",
          "modern%20classical",
          "ambient",
          "build%20up%20scenes",
          "acoustic%20group",
          "solo%20piano",
          "corporate",
          "solo%20instruments",
          "rnb",
          "action",
          "intro%2Foutro",
          "rock",
          "folk",
          "adventure",
          "vocal",
          "mystery",
          "chase%20scene",
          "indie%20pop",
          "pulses",
          "meditation%2Fspiritual",
          "small%20emotions",
          "alternative",
          "nostalgia",
          "trap",
          "high%20drones",
          "mainstream%20hip%20hop",
          "solo%20classical%20instruments");

  public PixabayMusicCollector(
      RestClient restClient, Environment environment, RedisDataStorage redisDataStorage) {
    super(restClient, environment, redisDataStorage);
  }

  @Override
  protected String getBaseUrlKey() {
    return ApiMetadata.PIXABAY_MUSIC.getUrlPropertyKey();
  }

  @Override
  protected String getStorageKey() {
    return ApiMetadata.PIXABAY_MUSIC.getKey();
  }

  @Override
  protected List<String> getFilters() {
    return PIXABAY_MUSIC_GENRES;
  }

  @Override
  protected void appendQueryParams(UriComponentsBuilder builder, String genre) {
    builder.queryParam("genre", genre);
  }

  @Override
  protected ParameterizedTypeReference<CustomPixabayMusicResponse<PixabayMusicResult>>
      getResponseTypeReference() {
    return new ParameterizedTypeReference<CustomPixabayMusicResponse<PixabayMusicResult>>() {};
  }
}
