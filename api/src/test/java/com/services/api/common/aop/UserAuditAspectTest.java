package com.services.api.common.aop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.services.api.common.annotation.AuditAction;
import com.services.api.common.annotation.AuditLog;
import com.services.api.common.security.service.MemberService;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

class UserAuditAspectTest {

  private MemberService memberService;
  private ListAppender<ILoggingEvent> listAppender;
  private Logger userAuditLogger;
  private SampleService proxy;

  static class SampleService {

    @AuditLog(action = AuditAction.VIEW_COMPANY_STAT, target = "#companySlug")
    public String getCompanySummary(String companySlug) {
      return "summary";
    }

    @AuditLog(action = AuditAction.DELETE_APPLICATION, target = "#id")
    public void deleteApplication(UUID id) {}

    @AuditLog(action = AuditAction.SEARCH_COMPANY)
    public void searchCompanies() {}
  }

  @BeforeEach
  void setUp() {
    memberService = mock(MemberService.class);
    userAuditLogger = (Logger) LoggerFactory.getLogger("USER_AUDIT_LOGGER");
    listAppender = new ListAppender<>();
    listAppender.start();
    userAuditLogger.addAppender(listAppender);

    SampleService target = new SampleService();
    AspectJProxyFactory factory = new AspectJProxyFactory(target);
    UserAuditAspect aspect = new UserAuditAspect(memberService);
    factory.addAspect(aspect);
    proxy = factory.getProxy();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
    userAuditLogger.detachAppender(listAppender);
  }

  @Test
  @DisplayName("로그인 유저인 경우 memberId(UUID)와 action, target이 로깅된다")
  void auditLogAuthenticatedUser() {
    String email = "test@example.com";
    String memberId = UUID.randomUUID().toString();
    given(memberService.getMemberIdByEmail(email)).willReturn(memberId);

    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(email, null, Collections.emptyList());
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(auth);
    SecurityContextHolder.setContext(context);

    proxy.getCompanySummary("naver");

    assertThat(listAppender.list).hasSize(1);
    String formattedMessage = listAppender.list.get(0).getFormattedMessage();
    assertThat(formattedMessage)
        .isEqualTo("[USER_AUDIT] memberId=" + memberId + " action=VIEW_COMPANY_STAT target=naver");
  }

  @Test
  @DisplayName("비로그인 유저인 경우 memberId가 ANONYMOUS로 로깅된다")
  void auditLogAnonymousUser() {
    SecurityContextHolder.clearContext();

    proxy.searchCompanies();

    assertThat(listAppender.list).hasSize(1);
    String formattedMessage = listAppender.list.get(0).getFormattedMessage();
    assertThat(formattedMessage)
        .isEqualTo("[USER_AUDIT] memberId=ANONYMOUS action=SEARCH_COMPANY target=");
  }

  @Test
  @DisplayName("SpEL 파라미터가 UUID 객체인 경우 target이 올바르게 문자열 변환되어 로깅된다")
  void auditLogSpelUuidTarget() {
    String email = "user@example.com";
    String memberId = UUID.randomUUID().toString();
    given(memberService.getMemberIdByEmail(email)).willReturn(memberId);

    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(email, null, Collections.emptyList());
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(auth);
    SecurityContextHolder.setContext(context);

    UUID appId = UUID.randomUUID();
    proxy.deleteApplication(appId);

    assertThat(listAppender.list).hasSize(1);
    String formattedMessage = listAppender.list.get(0).getFormattedMessage();
    assertThat(formattedMessage)
        .isEqualTo(
            "[USER_AUDIT] memberId=" + memberId + " action=DELETE_APPLICATION target=" + appId);
  }

  @Test
  @DisplayName("MemberService에서 예외가 발생하면 memberId는 ANONYMOUS로 fallback 로깅된다")
  void auditLogMemberServiceExceptionFallback() {
    String email = "error@example.com";
    given(memberService.getMemberIdByEmail(email))
        .willThrow(new RuntimeException("DB Connection Error"));

    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(email, null, Collections.emptyList());
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(auth);
    SecurityContextHolder.setContext(context);

    proxy.searchCompanies();

    assertThat(listAppender.list).hasSize(1);
    String formattedMessage = listAppender.list.get(0).getFormattedMessage();
    assertThat(formattedMessage)
        .isEqualTo("[USER_AUDIT] memberId=ANONYMOUS action=SEARCH_COMPANY target=");
  }
}
