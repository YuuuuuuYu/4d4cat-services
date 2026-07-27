package com.services.api.applydays.controller;

import com.services.api.applydays.dto.PaymentMethodResponse;
import com.services.api.applydays.dto.RegisterPaymentMethodRequest;
import com.services.core.applydays.entity.subscription.ApplyDaysPaymentMethod;
import com.services.core.applydays.service.ApplyDaysPaymentMethodCommandService;
import com.services.core.applydays.service.ApplyDaysPaymentMethodQueryService;
import com.services.core.common.dto.BaseResponse;
import com.services.core.common.exception.ErrorCode;
import com.services.core.common.exception.NotFoundException;
import com.services.core.common.exception.UnauthorizedException;
import com.services.core.common.persistence.entity.member.Member;
import com.services.core.common.persistence.repository.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/applydays/payment-methods")
@RequiredArgsConstructor
public class ApplyDaysPaymentMethodController {

  private final ApplyDaysPaymentMethodQueryService paymentMethodQueryService;
  private final ApplyDaysPaymentMethodCommandService paymentMethodCommandService;
  private final MemberRepository memberRepository;

  @GetMapping("/me")
  public BaseResponse<PaymentMethodResponse> getMyPaymentMethod(Authentication authentication) {
    if (authentication == null) {
      log.warn("Unauthorized request to /applydays/payment-methods/me");
      throw new UnauthorizedException(ErrorCode.UNAUTHORIZED);
    }
    String email = authentication.getName();
    log.info("Fetching payment method for user: {}", email);

    Member member =
        memberRepository
            .findByEmail(email)
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

    return paymentMethodQueryService
        .findByMemberId(member.getId())
        .map(pm -> BaseResponse.of(HttpStatus.OK, PaymentMethodResponse.from(pm)))
        .orElseGet(() -> BaseResponse.of(HttpStatus.OK, null));
  }

  @PostMapping("/register")
  public BaseResponse<PaymentMethodResponse> registerPaymentMethod(
      Authentication authentication, @RequestBody RegisterPaymentMethodRequest request) {
    if (authentication == null) {
      throw new UnauthorizedException(ErrorCode.UNAUTHORIZED);
    }
    String email = authentication.getName();
    log.info("Registering payment method for user: {}", email);

    Member member =
        memberRepository
            .findByEmail(email)
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

    boolean isDefault = request.isDefault() == null || request.isDefault();
    ApplyDaysPaymentMethod pm =
        paymentMethodCommandService.registerPaymentMethod(
            member.getId(),
            request.billingKey(),
            request.cardCompany(),
            request.cardNumberMasked(),
            isDefault);

    return BaseResponse.of(HttpStatus.CREATED, PaymentMethodResponse.from(pm));
  }

  @DeleteMapping
  public BaseResponse<Void> deletePaymentMethod(Authentication authentication) {
    if (authentication == null) {
      throw new UnauthorizedException(ErrorCode.UNAUTHORIZED);
    }
    String email = authentication.getName();
    log.info("Deleting payment method for user: {}", email);

    Member member =
        memberRepository
            .findByEmail(email)
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

    paymentMethodCommandService.deletePaymentMethod(member.getId());

    return BaseResponse.of(HttpStatus.OK, null);
  }
}
