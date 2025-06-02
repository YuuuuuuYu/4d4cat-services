package com.services.pixabay.application.dto.result;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PixabayVideoResult {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private Integer id;
    private String pageURL;
    private String type;
    private String tags;
    private Integer duration;
    private Object videos;
    private Integer views;
    private Integer downloads;
    private Integer likes;
    private Integer comments;
    private Integer user_id;
    private String user;
    private String userImageURL;
    private Boolean noAiTraining;
    
    public static PixabayVideoResult create(Integer id, String pageURL, String type, String tags, Integer duration,
                              Object videos, Integer views, Integer downloads, Integer likes,
                              Integer comments, Integer user_id, String user, String userImageURL,
                              Boolean noAiTraining) {
        validateVideos(videos);
        return PixabayVideoResult.builder()
                .id(id)
                .pageURL(pageURL)
                .type(type)
                .tags(tags)
                .duration(duration)
                .videos(videos)
                .views(views)
                .downloads(downloads)
                .likes(likes)
                .comments(comments)
                .user_id(user_id)
                .user(user)
                .userImageURL(userImageURL)
                .noAiTraining(noAiTraining)
                .build();
    }

    private static void validateVideos(Object videos) {
        if (videos == null) {
            throw new IllegalArgumentException("Videos cannot be null");
        }
        JsonNode videosNode = convertToJsonNode(videos);
        
        if (!videosNode.isArray()) {
            throw new IllegalArgumentException("Videos must be an array");
        }
        
        if (videosNode.size() == 0) {
            throw new IllegalArgumentException("Videos array cannot be empty");
        }
    }

    private static JsonNode convertToJsonNode(Object videos) {
        try {
            if (videos instanceof String) {
                return objectMapper.readTree(String.valueOf(videos));
            } else if (videos instanceof JsonNode) {
                return (JsonNode) videos;
            } else {
                return objectMapper.valueToTree(videos);
            }
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid JSON format for videos", e);
        }
    }
}
