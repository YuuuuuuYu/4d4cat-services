package com.services.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class WebUtils {

  private WebUtils() {
    // 인스턴스화 방지
  }

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

        // 쉼표로 구분된 여러 IP가 있는 경우 첫 번째 것을 사용
        if (ip.contains(",")) {
          ip = ip.split(",")[0].trim();
        }

        return ip;
      }
    } catch (Exception e) {

    }
    return "Unknown";
  }

  private static boolean isInvalidIp(String ip) {
    return ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip);
  }
}
