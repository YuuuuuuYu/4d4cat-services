package com.services.pixabay.fixture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.services.pixabay.application.dto.result.PixabayMusicResult;
import com.services.pixabay.application.dto.result.PixabayVideoResult;
import com.services.pixabay.presentation.dto.PixabayResponse;

public class PixabayTestFixtures {
    
    public static final String SAMPLE_VIDEOS_JSON = """
        {
            "small": "https://cdn.pixabay.com/video/2015/08/08/125-135736646_small.mp4",
            "medium": "https://cdn.pixabay.com/video/2015/08/08/125-135736646_medium.mp4",
            "large": "https://cdn.pixabay.com/video/2015/08/08/125-135736646_large.mp4"
        }
            """;
    
    public static PixabayVideoResult createDefaultVideoResult(int id) {
        return PixabayVideoResult.builder()
            .id(id)
            .pageURL("https://pixabay.com/videos/id-125/")
            .type("video")
            .tags("nature, video")
            .duration(120)
            .videos(SAMPLE_VIDEOS_JSON)
            .views(1000)
            .downloads(100)
            .likes(50)
            .comments(10)
            .user_id(123)
            .user("Coverr-Free-Footage")
            .userImageURL("https://cdn.pixabay.com/user/2015/10/16/09-28-45-303_250x250.png")
            .noAiTraining(false)
            .build();
    }

    public static void setupRestTemplateToReturnSingleVideo(RestTemplate restTemplate, int id) {
        String total = "1";
        String totalHits = "1";
        PixabayVideoResult videoResult = createDefaultVideoResult(id);
        PixabayResponse<PixabayVideoResult> response = new PixabayResponse<>(total, totalHits, List.of(videoResult));

        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            isNull(),
            any(ParameterizedTypeReference.class)
        )).thenReturn(ResponseEntity.ok(response));
    }

    public static PixabayMusicResult createDefaultMusicResult(long id) {
        return PixabayMusicResult.builder()
            .id(id)
            .title("Sample Music Title")
            .duration(180)
            .author("Sample Author")
            .tags(List.of("tag1", "tag2", "tag3"))
            .download_url("https://pixabay.com/music/download/id-1111.mp3")
            .thumbnail_url("https://cdn.pixabay.com/audio/2025/06/03/09-20-08-320_200x200.png")
            .url("/music/genre-1111/")
            .build();
    }
}
