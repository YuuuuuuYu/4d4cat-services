package com.services.core.dto;

import java.util.List;

public interface ApiResponse<T> {

  List<T> getItems();

  String getTotal();
}
