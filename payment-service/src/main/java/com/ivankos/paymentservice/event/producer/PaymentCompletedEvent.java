package com.ivankos.paymentservice.event.producer;

import java.time.Instant;
import java.util.UUID;

public record PaymentCompletedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID orderId) implements PaymentEvent {
}
