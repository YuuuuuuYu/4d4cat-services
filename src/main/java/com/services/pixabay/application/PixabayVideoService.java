package com.services.pixabay.application;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.client.RestTemplate;

import com.services.common.application.DataInitializationService;
import com.services.common.infrastructure.ApiMetadata;
import com.services.common.infrastructure.DataStorage;
import com.services.pixabay.application.dto.request.PixabayVideoRequest;
import com.services.pixabay.application.dto.result.PixabayVideoResult;
import com.services.pixabay.presentation.dto.PixabayResponse;

@Service
public class PixabayVideoService extends DataInitializationService<PixabayVideoResult, PixabayVideoRequest, PixabayResponse<PixabayVideoResult>> {

    private static final List<String> PIXABAY_VIDEO_CATEGORIES = List.of(
            "backgrounds", "fashion", "nature", "science", "education", "feelings", "health", "people", "religion", "places",
                    "animals", "industry", "computer", "food", "sports", "transportation", "travel", "buildings", "business", "music");

    @Value("${pixabay.key}")
    protected String key;

    public PixabayVideoService(RestTemplate restTemplate) {
        super(restTemplate);
    }

    @Override
    protected String getBaseUrl() {
        return ApiMetadata.PIXABAY_VIDEOS.getUrl();
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
    protected PixabayVideoRequest createParameters(String category) {
        return new PixabayVideoRequest(key, category);
    }

    @Override
    protected ParameterizedTypeReference<PixabayResponse<PixabayVideoResult>> getResponseTypeReference() {
        return new ParameterizedTypeReference<PixabayResponse<PixabayVideoResult>>() {};
    }

    @Override
    public void addRandomElementToModel(Model model) {
        DataStorage.getRandomElement(getStorageKey(), PixabayVideoResult.class)
            .ifPresent(element -> model.addAttribute(ApiMetadata.PIXABAY_VIDEOS.getAttributeName(), element));
    }
}
