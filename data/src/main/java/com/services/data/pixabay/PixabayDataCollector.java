package com.services.data.pixabay;

import com.services.core.dto.ApiResponse;
import com.services.core.infrastructure.RedisDataStorage;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
public abstract class PixabayDataCollector<T, R extends ApiResponse<T>> {

  protected final RestClient restClient;
  protected final Environment environment;
  protected final RedisDataStorage redisDataStorage;

  public PixabayDataCollector(
      RestClient restClient, Environment environment, RedisDataStorage redisDataStorage) {
    this.restClient = restClient;
    this.environment = environment;
    this.redisDataStorage = redisDataStorage;
  }

  public void collectAndStore() {
    log.info("Starting data collection for: {}", getStorageKey());
    List<T> dataList = fetchAllData();
    redisDataStorage.setListData(getStorageKey(), dataList);
    log.info(
        "Completed data collection for: {} - {} items stored", getStorageKey(), dataList.size());
  }

  protected List<T> fetchAllData() {
    List<String> filters = getFilters();

    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      List<CompletableFuture<Optional<R>>> futures =
          filters.stream()
              .map(
                  filter ->
                      CompletableFuture.supplyAsync(() -> fetchDataForFilter(filter), executor))
              .toList();

      return futures.stream()
          .map(CompletableFuture::join)
          .filter(Optional::isPresent)
          .map(Optional::get)
          .flatMap(response -> response.getItems().stream())
          .toList();
    }
  }

  protected Optional<R> fetchDataForFilter(String filter) {
    try {
      String uri = buildUri(filter).toUriString();
      R result = restClient.get().uri(uri).retrieve().body(getResponseTypeReference());
      return Optional.ofNullable(result);
    } catch (Exception e) {
      log.warn("Failed to fetch data for filter '{}': {}", filter, e.getMessage());
      return Optional.empty();
    }
  }

  protected UriComponentsBuilder buildUri(String filter) {
    String baseUrl = environment.getProperty(getBaseUrlKey());
    UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl);
    appendQueryParams(builder, filter);
    return builder;
  }

  protected abstract String getBaseUrlKey();

  protected abstract String getStorageKey();

  protected abstract List<String> getFilters();

  protected abstract void appendQueryParams(UriComponentsBuilder builder, String filter);

  protected abstract ParameterizedTypeReference<R> getResponseTypeReference();
}
