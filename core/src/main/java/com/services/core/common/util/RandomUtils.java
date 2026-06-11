package com.services.core.common.util;

import java.util.concurrent.ThreadLocalRandom;

public class RandomUtils {

  public static int generateRandomInt(int max) {
    if (max <= 0) {
      return 0;
    }
    return ThreadLocalRandom.current().nextInt(max);
  }

  public static String generateRandomAlphanumeric(int length) {
    String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < length; i++) {
      sb.append(chars.charAt(generateRandomInt(chars.length())));
    }
    return sb.toString();
  }
}
