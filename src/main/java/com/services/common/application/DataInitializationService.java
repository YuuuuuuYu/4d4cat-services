package com.services.common.application;

import com.services.common.application.dto.ParameterBuilder;
import com.services.common.application.exception.BadGatewayException;
import com.services.common.application.exception.ErrorCode;
import com.services.common.application.exception.InternalServerException;
import com.services.common.application.exception.NotFoundException;
import com.services.common.infrastructure.DataStorage;
import com.services.common.presentation.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
@Slf4j
public abstract class DataInitializationService<T, P extends ParameterBuilder, R extends ApiResponse<T>> {

    private final Executor executor;
    protected final RestTemplate restTemplate;

    public DataInitializationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.executor = Executors.newFixedThreadPool(10);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void setDataStorage() {
        List<T> dataList = getFetchDataList();
        DataStorage.setData(getStorageKey(), dataList);
    }

    public List<T> getFetchDataList() {
        List<String> filters = getFilters();
        
        // 병렬로 API 호출 실행
        List<CompletableFuture<Optional<R>>> futures = filters.stream()
                .map(filter -> CompletableFuture
                        .supplyAsync(() -> fetchDataForFilter(filter), executor)
                        .exceptionally(throwable -> {
                            log.warn("Failed to fetch data for filter '{}': {}", filter, throwable.getMessage());
                            return Optional.empty();
                        }))
                .toList();
        
        // 모든 CompletableFuture 완료 대기 및 결과 수집
        return futures.stream()
                .map(CompletableFuture::join)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .flatMap(response -> response.getItems().stream())
                .collect(Collectors.toList());
    }

    private Optional<R> fetchDataForFilter(String filter) {
        try {
            P params = createParameters(filter);
            R result = getApiResponseBody(getResponseTypeReference(), params);
            return Optional.ofNullable(result);
        } catch (NotFoundException e) {
            return Optional.empty();
        } catch (BadGatewayException e) {
            return Optional.empty();
        } catch (Exception e) {
            throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private R getApiResponseBody(ParameterizedTypeReference<R> responseType, P params) {
        UriComponentsBuilder builder = getUriBuilder(params);
        ResponseEntity<R> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                responseType
        );
        return response.getBody();
    }

    private UriComponentsBuilder getUriBuilder(P params) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(getBaseUrl());
        if (Objects.nonNull(params)) {
            params.appendToBuilder(builder);
        }
        return builder;
    }

    protected abstract String getBaseUrl();
    protected abstract String getStorageKey();
    protected abstract List<String> getFilters();
    protected abstract P createParameters(String filter);
    protected abstract ParameterizedTypeReference<R> getResponseTypeReference();
    protected abstract T getRandomElement();
}
