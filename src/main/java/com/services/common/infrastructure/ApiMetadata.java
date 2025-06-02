package com.services.common.infrastructure;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ApiMetadata {
    
    PIXABAY_MUSIC("pixabayMusic", "pixabayMusic", "https://api.4d4cat.site/pixabay/music/search/filter"),
    PIXABAY_VIDEOS("pixabayVideos", "pixabayVideo", "https://pixabay.com/api/videos"),
    ;

    private final String key;
    private final String attributeName;
    private final String url;
}
