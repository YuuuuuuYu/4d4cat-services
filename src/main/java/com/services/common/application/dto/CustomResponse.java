package com.services.common.application.dto;

import java.util.List;

public record CustomResponse<T>(
        String query,
        String page,
        String total,
        List<T> results
        ) {

    public static <T> CustomResponse<T> of(String query, String page, String total, List<T> results) {
        return new CustomResponse<T>(query, page, total, results);
    }
    
}
