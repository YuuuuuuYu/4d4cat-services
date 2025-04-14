package com.services.pixabay.application;

import com.services.common.application.DataInitializationService;
import com.services.common.domain.DataStorage;
import com.services.pixabay.application.dto.PixabayResponse;
import com.services.pixabay.domain.PixabayVideo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class PixabayVideoService extends DataInitializationService {

    private static final String PIXABAY_VIDEO_URL = "https://pixabay.com/api/videos/";

    @Value("${pixabay.key}")
    private String key;

    public PixabayVideoService(RestTemplate restTemplate) {
        super(restTemplate);
    }

    @Override
    protected String getBaseUrl() {
        return PIXABAY_VIDEO_URL + "?key=" + key;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeVideoData() {
        try {
            PixabayResponse<PixabayVideo> response = get(new ParameterizedTypeReference<PixabayResponse>() {});
            if (response != null && response.hits() != null) {
                DataStorage.setData("pixabayVideos", response.hits());
            }
        } catch (Exception e) {
            System.err.println("Error fetching data from Pixabay: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
