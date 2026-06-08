package com.services.core.common.notification.email.woorimail;

import com.services.core.common.dto.BaseResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class WoorimailClient {

  private final WoorimailProperties properties;
  private final RestTemplate restTemplate;

  public BaseResponse<String> sendEmail(
      String toEmail, String toNickname, String subject, String content) {
    String protocol = "Y".equalsIgnoreCase(properties.getSsl()) ? "https" : "http";
    int port = properties.getSslPort();
    String portStr = (port == 80 || port == 443) ? "" : ":" + port;
    String url =
        String.format(
            "%s://%s%s%s", protocol, properties.getServerUrl(), portStr, properties.getEndpoint());

    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("act", properties.getAct());
    params.add("authkey", properties.getAuthkey());
    params.add("mid", properties.getMid());
    params.add("domain", properties.getDomain());
    params.add("type", properties.getType());

    params.add("title", subject);
    params.add("content", content);
    params.add("receiver_nickname", toNickname);
    params.add("receiver_email", toEmail);
    params.add(
        "member_regdate",
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));

    params.add("sender_email", properties.getSenderEmail());
    params.add(
        "sender_nickname",
        properties.getSenderNickname() != null ? properties.getSenderNickname() : "");
    params.add("wms_domain", properties.getWmsDomain());
    params.add("wms_nick", properties.getWmsNick() != null ? properties.getWmsNick() : "");
    params.add("callback", "");

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);

    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

    try {
      log.info("Sending email to {} via URL: {}", toEmail, url);
      String response = restTemplate.postForObject(url, request, String.class);
      log.info("Woorimail response: {}", response);
      return BaseResponse.of(HttpStatus.OK, response);
    } catch (Exception e) {
      log.error("Failed to send email via Woorimail to: {}", toEmail, e);
      return BaseResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, "WOORIMAIL_ERROR", e.getMessage());
    }
  }
}
