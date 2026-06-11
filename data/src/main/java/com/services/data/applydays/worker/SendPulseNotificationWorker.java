package com.services.data.applydays.worker;

import com.services.core.applydays.entity.Application;
import com.services.core.applydays.entity.VerificationRequest;
import com.services.core.applydays.entity.VerificationStatus;
import com.services.core.applydays.repository.VerificationRequestRepository;
import com.services.core.common.infrastructure.external.sendpulse.SendPulseEmailClient;
import com.services.core.common.infrastructure.external.sendpulse.dto.SendPulseEmailRequest;
import com.services.core.common.persistence.entity.Company;
import com.services.core.common.persistence.entity.member.Member;
import com.services.core.common.persistence.repository.CompanyRepository;
import io.github.resilience4j.ratelimiter.RateLimiter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SendPulseNotificationWorker {

  private final SendPulseEmailClient sendPulseEmailClient;
  private final CompanyRepository companyRepository;
  private final VerificationRequestRepository verificationRequestRepository;
  private final RateLimiter sendPulseRateLimiter;

  @Value("${app.notification.sendpulse.sender-email}")
  private String senderEmail;

  @Value("${app.notification.sendpulse.sender-name}")
  private String senderName;

  public void sendVerificationResultEmail(Member member, List<Application> applications) {
    long approvedCount =
        applications.stream()
            .filter(a -> a.getVerificationStatus() == VerificationStatus.APPROVED)
            .count();
    long rejectedCount =
        applications.stream()
            .filter(a -> a.getVerificationStatus() == VerificationStatus.REJECTED)
            .count();

    String subject = "지원서 인증 결과 안내";
    String htmlContent = buildHtmlContent(member.getName(), applications);
    String base64Html =
        Base64.getEncoder().encodeToString(htmlContent.getBytes(StandardCharsets.UTF_8));
    String textContent =
        String.format(
            "안녕하세요, %s님! 지원서 인증 결과가 도착했습니다. (승인: %d건, 거절: %d건) 상세 내역을 확인해주세요.",
            member.getName(), approvedCount, rejectedCount);

    SendPulseEmailRequest request =
        SendPulseEmailRequest.builder()
            .email(
                SendPulseEmailRequest.EmailDetail.builder()
                    .subject(subject)
                    .html(base64Html)
                    .text(textContent)
                    .from(
                        SendPulseEmailRequest.Participant.builder()
                            .name(senderName)
                            .email(senderEmail)
                            .build())
                    .to(
                        List.of(
                            SendPulseEmailRequest.Participant.builder()
                                .name(member.getName())
                                .email(member.getEmail())
                                .build()))
                    .build())
            .build();

    RateLimiter.waitForPermission(sendPulseRateLimiter);
    sendPulseEmailClient.sendEmail(request);
    log.info(
        "Sent verification result email (Approved: {}, Rejected: {}) to member {}",
        approvedCount,
        rejectedCount,
        member.getId());
  }

  private String buildHtmlContent(String name, List<Application> applications) {
    List<Application> approvedApps =
        applications.stream()
            .filter(a -> a.getVerificationStatus() == VerificationStatus.APPROVED)
            .toList();
    List<Application> rejectedApps =
        applications.stream()
            .filter(a -> a.getVerificationStatus() == VerificationStatus.REJECTED)
            .toList();

    StringBuilder sb = new StringBuilder();
    sb.append("<p>안녕하세요, ").append(name).append("님! 요청하신 지원서 인증 결과가 다음과 같이 처리되었습니다.</p>");

    if (!approvedApps.isEmpty()) {
      sb.append("<h3>[승인 완료 내역]</h3>");
      sb.append(
          "<table border=\"1\" cellpadding=\"5\" cellspacing=\"0\" style=\"border-collapse: collapse; width: 100%; margin-bottom: 20px;\">");
      sb.append("<thead>");
      sb.append("<tr style=\"background-color: #f2f2f2;\">");
      sb.append("<th>지원한 기업</th>");
      sb.append("<th>상세직군</th>");
      sb.append("<th>지원서조회패스워드</th>");
      sb.append("</tr>");
      sb.append("</thead>");
      sb.append("<tbody>");

      for (Application app : approvedApps) {
        sb.append("<tr>");
        sb.append("<td>").append(getCompanyName(app)).append("</td>");
        sb.append("<td>").append(app.getPositionDetail()).append("</td>");
        sb.append("<td><code style=\"background: #eee; padding: 2px 4px;\">")
            .append(app.getAccessPassword() != null ? app.getAccessPassword() : "")
            .append("</code></td>");
        sb.append("</tr>");
      }
      sb.append("</tbody></table>");
    }

    if (!rejectedApps.isEmpty()) {
      List<UUID> rejectedAppIds = rejectedApps.stream().map(Application::getId).toList();
      Map<UUID, String> rejectionReasons =
          verificationRequestRepository.findByApplicationIdIn(rejectedAppIds).stream()
              .collect(
                  Collectors.toMap(
                      VerificationRequest::getApplicationId,
                      vr -> vr.getRejectionReason() != null ? vr.getRejectionReason() : "사유 미지정"));

      sb.append("<h3>[거절 내역]</h3>");
      sb.append(
          "<table border=\"1\" cellpadding=\"5\" cellspacing=\"0\" style=\"border-collapse: collapse; width: 100%; margin-bottom: 20px;\">");
      sb.append("<thead>");
      sb.append("<tr style=\"background-color: #f2f2f2;\">");
      sb.append("<th>지원한 기업</th>");
      sb.append("<th>상세직군</th>");
      sb.append("<th>거절 사유</th>");
      sb.append("</tr>");
      sb.append("</thead>");
      sb.append("<tbody>");

      for (Application app : rejectedApps) {
        sb.append("<tr>");
        sb.append("<td>").append(getCompanyName(app)).append("</td>");
        sb.append("<td>").append(app.getPositionDetail()).append("</td>");
        sb.append("<td><span style=\"color: #d93025;\">")
            .append(rejectionReasons.getOrDefault(app.getId(), "사유 미지정"))
            .append("</span></td>");
        sb.append("</tr>");
      }
      sb.append("</tbody></table>");
    }

    sb.append("<p>감사합니다.</p>");
    return sb.toString();
  }

  private String getCompanyName(Application app) {
    return companyRepository
        .findBySlug(app.getCompanySlug())
        .map(Company::getName)
        .orElse(app.getCompanySlug());
  }
}
