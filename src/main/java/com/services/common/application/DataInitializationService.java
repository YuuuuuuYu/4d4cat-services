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
import com.services.common.exception.ErrorCode;
import com.services.common.exception.InternalServerException;
import com.services.common.exception.NotFoundException;
import com.services.common.infrastructure.DataStorage;
import com.services.common.presentation.dto.ApiResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public abstract class DataInitializationService<T, P extends ParameterBuilder, R extends ApiResponse<T>> {

    protected final RestTemplate restTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void setDataStorage() {
        List<T> dataList = getFetchDataList();
        DataStorage.setData(getStorageKey(), dataList);
    }

    private List<T> getFetchDataList() {
        return getFilters().stream()
                .map(this::fetchDataForFilter)
                .filter(Objects::nonNull)
                .flatMap(response -> response.getItems().stream())
                .toList();
    }

    private R fetchDataForFilter(String filter) {
        try {
            P params = createParameters(filter);
            return getApiResponseBody(getResponseTypeReference(), params);
        } catch (NotFoundException e) {
            return null;
        } catch (Exception e) {
            throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private R getApiResponseBody(ParameterizedTypeReference<R> responseType, P params) {
        UriComponentsBuilder builder = getUriBuilder(params);
        ResponseEntity<R> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                responseType
        );
        return response.getBody();
    }

    private UriComponentsBuilder getUriBuilder(P params) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(getBaseUrl());
        if (params != null) {
            params.appendToBuilder(builder);
        }
        return builder;
    }

    protected abstract String getBaseUrl();
    protected abstract String getStorageKey();
    protected abstract List<String> getFilters();
    protected abstract P createParameters(String filter);
    protected abstract ParameterizedTypeReference<R> getResponseTypeReference();
    public abstract void addRandomElementToModel(Model model);
}
