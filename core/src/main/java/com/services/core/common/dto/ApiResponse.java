package com.services.core.common.dto;

import java.util.List;

public interface ApiResponse<T> {

  List<T> getItems();

  String getTotal();
}
