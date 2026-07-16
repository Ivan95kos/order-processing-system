package com.ivankos.paymentservice.event.producer;

import com.ivankos.paymentservice.model.Payment;

import java.time.Instant;
import java.util.UUID;

public sealed interface PaymentEvent permits PaymentCompletedEvent, PaymentFailedEvent {
    UUID eventId();

    Instant occurredAt();

    PaymentStatusEvent paymentStatus();

    UUID orderId();

    static PaymentEvent from(Payment payment, UUID eventId, Instant occurredAt) {
        return switch (payment.getStatus()) {
            case COMPLETED -> new PaymentCompletedEvent(eventId, occurredAt, payment.getOrderId());
            case FAILED -> new PaymentFailedEvent(eventId, occurredAt, payment.getOrderId());
        };
    }
}
