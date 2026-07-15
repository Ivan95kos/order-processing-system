package com.ivankos.paymentservice.service;

import com.ivankos.paymentservice.event.OrderCreatedEvent;
import com.ivankos.paymentservice.model.Payment;
import com.ivankos.paymentservice.model.PaymentStatus;
import com.ivankos.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    @Value("${app.payment.failure-threshold}")
    private final BigDecimal failureThreshold;

    private final PaymentRepository paymentRepository;

    public void processOrderCreated(OrderCreatedEvent orderCreatedEvent) {
        if (paymentRepository.existsByOrderId(orderCreatedEvent.orderId())) {
            log.info("Payment already exists for orderId {}, skipping duplicate", orderCreatedEvent.orderId());
            return;
        }

        var paymentStatus = decideOutcome(orderCreatedEvent.totalAmount());
        var payment = Payment.create(
                orderCreatedEvent.orderId(),
                orderCreatedEvent.totalAmount(),
                paymentStatus);

        try {
            paymentRepository.save(payment);
        } catch (DataIntegrityViolationException exception) {
            log.info("Payment already exists for orderId {}, skipping duplicate", orderCreatedEvent.orderId());
            return;
        }

        // 5. (наступний блок) publish result
    }

    private PaymentStatus decideOutcome(BigDecimal amount) {
        return amount.compareTo(failureThreshold) >= 0
                ? PaymentStatus.FAILED
                : PaymentStatus.COMPLETED;
    }
}
