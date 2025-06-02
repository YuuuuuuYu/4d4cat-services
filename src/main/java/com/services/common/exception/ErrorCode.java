package com.services.common.exception;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // common errors
    INVALID_REQUEST("CO1000", "잘못된 요청입니다."),
    DATA_NOT_FOUND("CO1001", "데이터를 불러오지 못했습니다."),


    // pixabay video
    PIXABAY_VIDEO_NOT_FOUND("PV1000", "Pixabay 비디오를 찾을 수 없습니다."),

    // pixabay music
    PIXABAY_MUSIC_NOT_FOUND("PM1000", "Pixabay 음악을 찾을 수 없습니다."),
    ;

    private final String code;
    private final String message;
}
