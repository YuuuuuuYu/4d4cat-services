package com.services.common.infrastructure;

import com.services.common.application.exception.ErrorCode;
import com.services.common.application.exception.NotFoundException;
import com.services.common.util.RandomUtils;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DataStorage {

  private static final Map<String, Object> dataMap = new ConcurrentHashMap<>();

  public static <T> void setData(String key, T value) {
    dataMap.put(key, value);
  }

  @SuppressWarnings("unchecked")
  public static <T> Optional<List<T>> getListData(String key, Class<T> elementType) {
    Object value = dataMap.get(key);
    if (value instanceof List<?>) {
      List<?> list = (List<?>) value;
      if (list.isEmpty() || elementType.isInstance(list.get(0))) {
        return Optional.of((List<T>) list);
      }
    }
    return Optional.empty();
  }

  public static <T> Optional<T> getRandomElement(String key, Class<T> elementType) {
    return getListData(key, elementType)
        .filter(list -> !list.isEmpty())
        .map(list -> list.get(RandomUtils.generateRandomInt(list.size())));
  }

  public static <T> T getRandomElement(String key, Class<T> elementType, ErrorCode errorCode) {
    return getRandomElement(key, elementType).orElseThrow(() -> new NotFoundException(errorCode));
  }

  public static void clear() {
    dataMap.clear();
  }
}
