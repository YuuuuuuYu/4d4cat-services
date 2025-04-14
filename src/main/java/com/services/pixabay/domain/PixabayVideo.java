package com.services.pixabay.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class PixabayVideo {

    private String id;
    private String pageURL;
    private String type;
    private String tags;
    private String duration;
    private List<String> videos;
    private String views;
    private String downloads;
    private String likes;
    private String comments;
    private String userId;
    private String user;
    private String userImageURL;
}
