package com.ivankos.paymentservice.event.producer;

import java.time.Instant;
import java.util.UUID;

public record PaymentFailedEvent(
        UUID eventId,
        Instant occurredAt,
        PaymentStatusEvent paymentStatus,
        UUID orderId) implements PaymentEvent {

    public PaymentFailedEvent(UUID eventId,
                              Instant occurredAt,
                              UUID orderId) {
        this(eventId, occurredAt, PaymentStatusEvent.PAYMENT_FAILED, orderId);
    }
}
