package com.services.common.util;

public class RandomUtils {

    public static int generateRandomInt(int max) {
        return (int) (Math.random() * (max));
    }
}
