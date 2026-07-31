package com.services.core.applydays.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.services.core.applydays.entity.subscription.ApplyDaysPaymentMethod;
import com.services.core.applydays.event.PaymentMethodDeletedEvent;
import com.services.core.applydays.repository.ApplyDaysPaymentMethodRepository;
import com.services.core.common.exception.BadRequestException;
import com.services.core.common.infrastructure.external.portone.PortOneClient;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class ApplyDaysPaymentMethodCommandServiceTest {

  @Mock private ApplyDaysPaymentMethodRepository paymentMethodRepository;
  @Mock private PortOneClient portOneClient;
  @Mock private ApplicationEventPublisher eventPublisher;

  private ApplyDaysPaymentMethodCommandService commandService;

  @BeforeEach
  void setUp() {
    commandService =
        new ApplyDaysPaymentMethodCommandService(
            paymentMethodRepository, portOneClient, eventPublisher);
  }

  @Test
  @DisplayName("신규 결제 수단 등록")
  void registerPaymentMethod_new() {
    // given
    UUID memberId = UUID.randomUUID();
    given(paymentMethodRepository.findByMemberId(memberId)).willReturn(Optional.empty());
    given(paymentMethodRepository.save(any(ApplyDaysPaymentMethod.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    // when
    ApplyDaysPaymentMethod pm =
        commandService.registerPaymentMethod(memberId, "billing_new", "신한카드", "4330-****");

    // then
    assertThat(pm.getMemberId()).isEqualTo(memberId);
    assertThat(pm.getBillingKey()).isEqualTo("billing_new");
    assertThat(pm.getCardCompany()).isEqualTo("신한카드");
  }

  @Test
  @DisplayName("결제 수단 삭제 및 삭제 이벤트 발행")
  void deletePaymentMethod_success() {
    // given
    UUID memberId = UUID.randomUUID();
    ApplyDaysPaymentMethod pm =
        ApplyDaysPaymentMethod.builder()
            .memberId(memberId)
            .billingKey("billing_del")
            .cardCompany("국민카드")
            .cardNumberMasked("5520-****")
            .build();

    given(paymentMethodRepository.findByMemberIdAndDeletedFalse(memberId))
        .willReturn(Optional.of(pm));

    // when
    commandService.deletePaymentMethod(memberId);

    // then
    verify(portOneClient).deleteBillingKey("billing_del");
    verify(paymentMethodRepository).save(pm);
    assertThat(pm.isDeleted()).isTrue();

    ArgumentCaptor<PaymentMethodDeletedEvent> captor =
        ArgumentCaptor.forClass(PaymentMethodDeletedEvent.class);
    verify(eventPublisher).publishEvent(captor.capture());
    assertThat(captor.getValue().memberId()).isEqualTo(memberId);
    assertThat(captor.getValue().billingKey()).isEqualTo("billing_del");
  }

  @Test
  @DisplayName("존재하지 않는 결제 수단 삭제 시 예외 발생")
  void deletePaymentMethod_notFound() {
    // given
    UUID memberId = UUID.randomUUID();
    given(paymentMethodRepository.findByMemberIdAndDeletedFalse(memberId))
        .willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> commandService.deletePaymentMethod(memberId))
        .isInstanceOf(BadRequestException.class);
  }
}
