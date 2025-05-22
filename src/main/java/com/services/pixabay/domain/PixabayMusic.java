package com.services.pixabay.domain;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PixabayMusic {

    private final long id;
    private final String title;
    private final int duration;
    private final String author;
    private final List<String> tags;
    private final String downloadUrl;
    private final String url;
}
