package com.services.common.application;

import java.util.List;
import java.util.Objects;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.services.common.application.dto.ParameterBuilder;
import com.services.common.exception.BadRequestException;
import com.services.common.exception.ErrorCode;
import com.services.common.infrastructure.DataStorage;
import com.services.common.presentation.dto.ApiResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public abstract class DataInitializationService<T, P extends ParameterBuilder, R extends ApiResponse<T>> {

    protected final RestTemplate restTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeData() {
        List<T> dataList = getFilters().stream()
                .map(this::fetchDataForFilter)
                .filter(Objects::nonNull)
                .flatMap(response -> response.getItems().stream())
                .toList();
                
        DataStorage.setData(getStorageKey(), dataList);
    }

    protected R fetchDataForFilter(String filter) {
        try {
            P params = createParameters(filter);
            return getApiResponse(getResponseTypeReference(), params);
        } catch (Exception e) {
            throw new BadRequestException(ErrorCode.INVALID_REQUEST);
        }
    }

    protected R get(ParameterizedTypeReference<R> responseType, P params) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(getBaseUrl());
        if (params != null) {
            params.appendToBuilder(builder);
        }
        
        ResponseEntity<R> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                responseType
        );
        return response.getBody();
    }

    protected R getApiResponse(ParameterizedTypeReference<R> responseType, P params) {
        return get(responseType, params);
    }

    protected abstract String getBaseUrl();
    protected abstract String getStorageKey();
    protected abstract List<String> getFilters();
    protected abstract P createParameters(String filter);
    protected abstract ParameterizedTypeReference<R> getResponseTypeReference();
    public abstract void addRandomElementToModel(Model model);
}
