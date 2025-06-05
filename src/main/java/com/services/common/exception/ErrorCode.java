package com.services.common.exception;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // common errors
    INVALID_REQUEST("CO1000", "Invalid request"),
    DATA_NOT_FOUND("CO1001", "Data not found"),
    INTERNAL_SERVER_ERROR("CO1002", "Internal server error"),

    // pixabay video
    PIXABAY_VIDEO_NOT_FOUND("PV1000", "PixabayVideo data not found"),

    // pixabay music
    PIXABAY_MUSIC_NOT_FOUND("PM1000", "PixabayMusic data not found"),
    ;

    private final String code;
    private final String message;
}
