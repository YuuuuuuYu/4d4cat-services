package com.services.core.util;

import java.util.concurrent.ThreadLocalRandom;

public class RandomUtils {

  public static int generateRandomInt(int max) {
    if (max <= 0) {
      return 0;
    }
    return ThreadLocalRandom.current().nextInt(max);
  }
}
