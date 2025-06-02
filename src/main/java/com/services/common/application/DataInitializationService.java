package com.services.common.application;

import com.services.common.domain.ParameterBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public abstract class DataInitializationService {

    protected final RestTemplate restTemplate;

    protected <T, P extends ParameterBuilder> T get(ParameterizedTypeReference<T> responseType, P params) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(getBaseUrl());
        if (params != null) {
            params.appendToBuilder(builder);
        }

        ResponseEntity<T> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                responseType
        );
        return response.getBody();
    }

    protected abstract String getBaseUrl();
}
