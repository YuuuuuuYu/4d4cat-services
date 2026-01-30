package com.services.data.pixabay;

import com.services.core.aop.NotifyDiscord;
import com.services.core.infrastructure.ApiMetadata;
import com.services.core.infrastructure.RedisDataStorage;
import com.services.core.notification.DataCollectionResult;
import com.services.core.pixabay.dto.PixabayResponse;
import com.services.core.pixabay.dto.PixabayVideoResult;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class PixabayVideoCollector
    extends PixabayDataCollector<PixabayVideoResult, PixabayResponse<PixabayVideoResult>> {

  private static final List<String> PIXABAY_VIDEO_CATEGORIES =
      List.of(
          "backgrounds",
          "fashion",
          "nature",
          "science",
          "education",
          "feelings",
          "health",
          "people",
          "religion",
          "places",
          "animals",
          "industry",
          "computer",
          "food",
          "sports",
          "transportation",
          "travel",
          "buildings",
          "business",
          "music");

  @Value("${pixabay.key}")
  private String apiKey;

  public PixabayVideoCollector(
      RestClient restClient, Environment environment, RedisDataStorage redisDataStorage) {
    super(restClient, environment, redisDataStorage);
  }

  @Override
  @NotifyDiscord(taskName = "Pixabay 비디오 수집")
  public DataCollectionResult collectAndStore() {
    return super.collectAndStore();
  }

  @Override
  protected String getBaseUrlKey() {
    return ApiMetadata.PIXABAY_VIDEOS.getUrlPropertyKey();
  }

  @Override
  protected String getStorageKey() {
    return ApiMetadata.PIXABAY_VIDEOS.getKey();
  }

  @Override
  protected List<String> getFilters() {
    return PIXABAY_VIDEO_CATEGORIES;
  }

  @Override
  protected void appendQueryParams(UriComponentsBuilder builder, String category) {
    builder.queryParam("key", apiKey);
    builder.queryParam("category", category);
  }

  @Override
  protected ParameterizedTypeReference<PixabayResponse<PixabayVideoResult>>
      getResponseTypeReference() {
    return new ParameterizedTypeReference<>() {};
  }

  @Override
  protected String getDataType() {
    return "Video";
  }
}
