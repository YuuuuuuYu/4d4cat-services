package com.services.common.infrastructure;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.services.common.util.RandomUtils;

public class DataStorage {
    private static final Map<String, Object> dataMap = new ConcurrentHashMap<>();

    public static <T> void setData(String key, T value) {
        dataMap.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public static <T> Optional<T> getData(String key, Class<T> type) {
        Object value = dataMap.get(key);
        if (value != null && type.isInstance(value)) {
            return Optional.of((T) value);
        }
        return Optional.empty();
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
}