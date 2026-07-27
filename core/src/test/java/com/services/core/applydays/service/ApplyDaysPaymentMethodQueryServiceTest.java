package com.services.core.applydays.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.services.core.applydays.entity.subscription.ApplyDaysPaymentMethod;
import com.services.core.applydays.repository.ApplyDaysPaymentMethodRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApplyDaysPaymentMethodQueryServiceTest {

  @Mock private ApplyDaysPaymentMethodRepository paymentMethodRepository;

  private ApplyDaysPaymentMethodQueryService queryService;

  @BeforeEach
  void setUp() {
    queryService = new ApplyDaysPaymentMethodQueryService(paymentMethodRepository);
  }

  @Test
  @DisplayName("회원 ID로 삭제되지 않은 결제 수단 조회")
  void findByMemberId_success() {
    // given
    UUID memberId = UUID.randomUUID();
    ApplyDaysPaymentMethod pm =
        ApplyDaysPaymentMethod.builder()
            .memberId(memberId)
            .billingKey("billing_123")
            .cardCompany("KCP")
            .cardNumberMasked("1234-****")
            .isDefault(true)
            .build();

    given(paymentMethodRepository.findByMemberIdAndDeletedFalse(memberId))
        .willReturn(Optional.of(pm));

    // when
    Optional<ApplyDaysPaymentMethod> result = queryService.findByMemberId(memberId);

    // then
    assertThat(result).isPresent();
    assertThat(result.get().getBillingKey()).isEqualTo("billing_123");
  }

  @Test
  @DisplayName("결제 수단 존재 여부 확인")
  void hasPaymentMethod_true() {
    // given
    UUID memberId = UUID.randomUUID();
    given(paymentMethodRepository.existsByMemberIdAndDeletedFalse(memberId)).willReturn(true);

    // when & then
    assertThat(queryService.hasPaymentMethod(memberId)).isTrue();
  }
}
