package com.services.common.application;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public abstract class DataInitializationService {

    protected final RestTemplate restTemplate;

    protected <T> T get(ParameterizedTypeReference<T> responseType) {
        String url = getBaseUrl();
        ResponseEntity<T> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                responseType
        );
        return response.getBody();
    }

    protected abstract String getBaseUrl();
}
