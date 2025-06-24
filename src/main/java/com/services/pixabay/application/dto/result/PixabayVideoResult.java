package com.services.pixabay.application.dto.result;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PixabayVideoResult {

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
}
