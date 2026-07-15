package com.ivankos.paymentservice.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID eventId,
        Instant occurredAt,
        String eventType,
        UUID orderId,
        BigDecimal totalAmount
) {
}
