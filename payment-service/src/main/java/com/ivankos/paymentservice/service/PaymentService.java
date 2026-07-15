package com.ivankos.paymentservice.service;

import com.ivankos.paymentservice.event.consumer.OrderCreatedEvent;
import com.ivankos.paymentservice.event.producer.PaymentEvent;
import com.ivankos.paymentservice.model.Payment;
import com.ivankos.paymentservice.model.PaymentStatus;
import com.ivankos.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    @Value("${app.payment.failure-threshold}")
    private final BigDecimal failureThreshold;
    @Value("${app.kafka.topic.payment-events}")
    private final String paymentEventsTopic;

    private final KafkaTemplate<String, PaymentEvent> paymentEventTemplate;

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
            payment = paymentRepository.save(payment);
        } catch (DataIntegrityViolationException exception) {
            log.info("Payment already exists for orderId {}, skipping duplicate", orderCreatedEvent.orderId());
            return;
        }

        var paymentEvent = PaymentEvent.from(payment, UUID.randomUUID(), Instant.now());

        paymentEventTemplate.send(paymentEventsTopic, payment.getOrderId().toString(), paymentEvent);

        log.info("Payment {} for orderId {}, event published", payment.getStatus(), payment.getOrderId());
    }

    private PaymentStatus decideOutcome(BigDecimal amount) {
        return amount.compareTo(failureThreshold) >= 0
                ? PaymentStatus.FAILED
                : PaymentStatus.COMPLETED;
    }
}
