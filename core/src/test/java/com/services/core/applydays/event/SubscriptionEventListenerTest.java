package com.services.core.applydays.event;

import static org.mockito.Mockito.verify;

import com.services.core.applydays.service.ApplyDaysSubscriptionCommandService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscriptionEventListenerTest {

  @Mock private ApplyDaysSubscriptionCommandService subscriptionService;

  private SubscriptionEventListener eventListener;

  @BeforeEach
  void setUp() {
    eventListener = new SubscriptionEventListener(subscriptionService);
  }

  @Test
  @DisplayName("PaymentMethodDeletedEvent 수신 시 구독 해지 예약 서비스가 호출된다")
  void handlePaymentMethodDeleted_success() {
    // given
    UUID memberId = UUID.randomUUID();
    PaymentMethodDeletedEvent event = new PaymentMethodDeletedEvent(memberId, "billing_key_123");

    // when
    eventListener.handlePaymentMethodDeleted(event);

    // then
    verify(subscriptionService).scheduleCancellationOnCardDeleted(memberId);
  }
}
