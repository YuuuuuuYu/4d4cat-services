package com.services.api.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class WebUtils {

  private WebUtils() {}

  public static String getClientIp() {
    try {
      ServletRequestAttributes attributes =
          (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      if (attributes != null) {
        HttpServletRequest request = attributes.getRequest();
        String ip = request.getHeader("X-Forwarded-For");

        if (isInvalidIp(ip)) {
          ip = request.getHeader("Proxy-Client-IP");
        }
        if (isInvalidIp(ip)) {
          ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (isInvalidIp(ip)) {
          ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (isInvalidIp(ip)) {
          ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (isInvalidIp(ip)) {
          ip = request.getRemoteAddr();
        }

        if (ip.contains(",")) {
          ip = ip.split(",")[0].trim();
        }

        return ip;
      }
    } catch (Exception e) {
      // ignore
    }
    return "Unknown";
  }

  private static boolean isInvalidIp(String ip) {
    return ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip);
  }
}
