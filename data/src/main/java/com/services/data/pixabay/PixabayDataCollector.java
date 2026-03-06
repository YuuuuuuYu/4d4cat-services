package com.services.data.pixabay;

import com.services.core.dto.ApiResponse;
import com.services.core.infrastructure.RedisDataStorage;
import com.services.core.notification.DataCollectionResult;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
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
  protected final MeterRegistry registry;

  // Rate limiting configuration
  private static final long STAGGER_DELAY_MS = 1000; // 1 second between submissions

  private record FetchStatistics<T>(List<T> results, long successCount, long failureCount) {}

  public PixabayDataCollector(
      RestClient restClient,
      Environment environment,
      RedisDataStorage redisDataStorage,
      MeterRegistry registry) {
    this.restClient = restClient;
    this.environment = environment;
    this.redisDataStorage = redisDataStorage;
    this.registry = registry;
  }

  public DataCollectionResult collectAndStore() {
    long startTime = System.currentTimeMillis();
    log.info("Starting data collection for: {}", getStorageKey());

    FetchStatistics stats = fetchAllData();
    redisDataStorage.setData(getStorageKey(), stats.results());

    double durationSeconds = (System.currentTimeMillis() - startTime) / 1000.0;

    registry.counter("pixabay.collection.items", "type", getDataType()).increment(stats.results().size());
    registry.counter("pixabay.collection.filters", "type", getDataType(), "status", "success").increment(stats.successCount());
    registry.counter("pixabay.collection.filters", "type", getDataType(), "status", "failure").increment(stats.failureCount());

    log.info(
        "Completed data collection for: {} - {} items stored",
        getStorageKey(),
        stats.results().size());

    return new DataCollectionResult(
        getDataType() + " 데이터",
        stats.results().size(),
        stats.successCount(),
        stats.failureCount(),
        durationSeconds);
  }

  protected FetchStatistics fetchAllData() {
    List<String> filters = getFilters();
    log.info("Starting staggered parallel data collection for {} filters.", filters.size());

    List<CompletableFuture<Optional<R>>> futures = new ArrayList<>();

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      for (String filter : filters) {
        CompletableFuture<Optional<R>> future =
            CompletableFuture.supplyAsync(
                () -> {
                  log.info("Processing filter '{}'", filter);
                  return fetchDataForFilter(filter);
                },
                executor);
        futures.add(future);

        // Add a short delay before submitting the next task
        try {
          TimeUnit.MILLISECONDS.sleep(STAGGER_DELAY_MS);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          log.error("Stagger delay interrupted. Stopping submission of new tasks.");
          break;
        }
      }

      log.info("All {} tasks submitted. Waiting for completion...", futures.size());
      CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

      List<T> allResults =
          futures.stream()
              .map(CompletableFuture::join)
              .filter(Optional::isPresent)
              .map(Optional::get)
              .flatMap(response -> response.getItems().stream())
              .collect(Collectors.toList());

      long successCount = futures.stream().filter(f -> f.join().isPresent()).count();
      long failureCount = filters.size() - successCount;
      log.info(
          "Data collection completed: {} items from {}/{} successful filters ({} failed)",
          allResults.size(),
          successCount,
          filters.size(),
          failureCount);

      return new FetchStatistics(allResults, successCount, failureCount);
    }
  }

  protected Optional<R> fetchDataForFilter(String filter) {
    try {
      log.debug("Fetching data for filter '{}'", filter);
      String uri = buildUri(filter).toUriString();
      R result = restClient.get().uri(uri).retrieve().body(getResponseTypeReference());
      log.debug("Successfully fetched data for filter '{}'", filter);
      return Optional.ofNullable(result);
    } catch (Exception e) {
      log.error("Failed to fetch data for filter '{}'", filter, e);
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

  protected abstract String getDataType();
}
