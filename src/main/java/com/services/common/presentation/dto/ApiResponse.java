package com.services.common.presentation.dto;

import java.util.List;

public interface ApiResponse<T> {

  List<T> getItems();

  String getTotal();
}
