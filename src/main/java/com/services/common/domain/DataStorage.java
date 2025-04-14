package com.services.common.domain;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DataStorage {

    private static final Map<String, Object> dataMap = new ConcurrentHashMap<>();

    public static void setData(String key, Object value) {
        dataMap.put(key, value);
    }

    public static Object getData(String key) {
        return dataMap.get(key);
    }
}