package com.services.common.util;

public class RandomUtils {

    public static int generateRandomInt(int max) {
        long timestamp = System.currentTimeMillis();
        return (int) (timestamp % max);
    }
}
