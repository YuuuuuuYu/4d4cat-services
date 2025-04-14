package com.services.pixabay.application.dto;

import java.util.List;

public record PixabayResponse<T>(
        String total,
        String totalHits,
        List<T> hits
        ) {
    public static <T> PixabayResponse<T> of(String total, String totalHits, List<T> hits) {
        return new PixabayResponse(total, totalHits, hits);
    }
}
