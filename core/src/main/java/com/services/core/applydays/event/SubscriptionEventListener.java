package com.services.core.applydays.event;

import com.services.core.applydays.service.ApplyDaysSubscriptionCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionEventListener {

  private final ApplyDaysSubscriptionCommandService subscriptionService;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handlePaymentMethodDeleted(PaymentMethodDeletedEvent event) {
    log.info("Received PaymentMethodDeletedEvent for memberId={}", event.memberId());
    subscriptionService.scheduleCancellationOnCardDeleted(event.memberId());
  }
}
