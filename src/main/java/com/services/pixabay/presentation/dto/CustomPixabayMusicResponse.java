package com.services.pixabay.presentation.dto;

import com.services.common.presentation.dto.ApiResponse;
import java.util.List;

public record CustomPixabayMusicResponse<T>(
    String query, String page, String total, List<T> results) implements ApiResponse<T> {

  @Override
  public List<T> getItems() {
    return results;
  }

  @Override
  public String getTotal() {
    return total;
  }

  public static <T> CustomPixabayMusicResponse<T> of(
      String query, String page, String total, List<T> results) {
    return new CustomPixabayMusicResponse<T>(query, page, total, results);
  }
}
