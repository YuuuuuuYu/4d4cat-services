package com.services.api.common.aop;

import com.services.api.common.annotation.AuditLog;
import com.services.api.common.security.dto.SessionUser;
import com.services.api.common.security.service.MemberService;
import java.lang.reflect.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class UserAuditAspect {

  private static final Logger USER_AUDIT_LOGGER = LoggerFactory.getLogger("USER_AUDIT_LOGGER");
  private static final String ANONYMOUS = "ANONYMOUS";

  private final MemberService memberService;
  private final ParameterNameDiscoverer parameterNameDiscoverer =
      new DefaultParameterNameDiscoverer();
  private final ExpressionParser expressionParser = new SpelExpressionParser();

  @Around("@annotation(auditLog)")
  public Object logUserAudit(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
    String memberId = resolveMemberId();
    String target = resolveTarget(joinPoint, auditLog.target());

    Object result = joinPoint.proceed();

    USER_AUDIT_LOGGER.info(
        "[USER_AUDIT] memberId={} action={} target={}", memberId, auditLog.action(), target);

    return result;
  }

  private String resolveMemberId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      return ANONYMOUS;
    }

    Object principal = authentication.getPrincipal();
    if (ANONYMOUS.equalsIgnoreCase(authentication.getName()) || "anonymousUser".equals(principal)) {
      return ANONYMOUS;
    }

    String email = null;
    if (principal instanceof SessionUser sessionUser) {
      email = sessionUser.getEmail();
    } else if (principal instanceof String principalString) {
      email = principalString;
    } else {
      email = authentication.getName();
    }

    if (!StringUtils.hasText(email) || ANONYMOUS.equalsIgnoreCase(email)) {
      return ANONYMOUS;
    }

    try {
      return memberService.getMemberIdByEmail(email);
    } catch (Exception e) {
      log.debug("Could not resolve member UUID for email: {}", email, e);
      return ANONYMOUS;
    }
  }

  private String resolveTarget(ProceedingJoinPoint joinPoint, String expressionStr) {
    if (!StringUtils.hasText(expressionStr)) {
      return "";
    }

    try {
      MethodSignature signature = (MethodSignature) joinPoint.getSignature();
      Method method = signature.getMethod();
      Object[] args = joinPoint.getArgs();

      SimpleEvaluationContext.Builder contextBuilder =
          SimpleEvaluationContext.forReadOnlyDataBinding();
      EvaluationContext context = contextBuilder.build();

      String[] paramNames = parameterNameDiscoverer.getParameterNames(method);
      if (paramNames != null) {
        for (int i = 0; i < paramNames.length; i++) {
          context.setVariable(paramNames[i], args[i]);
        }
      }

      Object value = expressionParser.parseExpression(expressionStr).getValue(context);
      return value != null ? value.toString() : "";
    } catch (Exception e) {
      log.debug("Failed to evaluate SpEL expression: {}", expressionStr, e);
      return expressionStr;
    }
  }
}
