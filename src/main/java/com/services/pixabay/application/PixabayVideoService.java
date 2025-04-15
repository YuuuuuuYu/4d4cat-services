package com.services.pixabay.application;

import com.services.common.application.DataInitializationService;
import com.services.common.domain.DataStorage;
import com.services.pixabay.application.dto.PixabayResponse;
import com.services.pixabay.application.dto.PixabayVideoParameter;
import com.services.pixabay.domain.PixabayVideo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class PixabayVideoService extends DataInitializationService {

    private static final String PIXABAY_VIDEO_URL = "https://pixabay.com/api/videos";
    private static final String[] PIXABAY_VIDEO_CATEGORIES =
            {"backgrounds", "fashion", "nature", "science", "education", "feelings", "health", "people", "religion", "places",
                    "animals", "industry", "computer", "food", "sports", "transportation", "travel", "buildings", "business", "music"};

    @Value("${pixabay.key}")
    protected String key;

    public PixabayVideoService(RestTemplate restTemplate) {
        super(restTemplate);
    }

    @Override
    protected String getBaseUrl() {
        return PIXABAY_VIDEO_URL;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeVideoData() {
        List<PixabayVideo> videoList = new ArrayList<>();
        for (String category : PIXABAY_VIDEO_CATEGORIES) {
            try {
                PixabayVideoParameter params = PixabayVideoParameter.builder()
                                                    .key(key)
                                                    .category(category)
                                                    .build();
                PixabayResponse<PixabayVideo> response = get(new ParameterizedTypeReference<PixabayResponse>() {}, params);
                if (response != null && response.hits() != null) {
                    videoList.addAll(response.hits());
                }
            } catch (Exception e) {
                System.err.println("Error fetching data from Pixabay: " + e.getMessage());
                e.printStackTrace();
            }
        }
        DataStorage.setData("pixabayVideos", videoList);
    }
}
