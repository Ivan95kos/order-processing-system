package com.ivankos.orderservice.event.producer;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID eventId,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant occurredAt,
        OrderEventType eventType,
        UUID orderId,
        UUID customerId,
        List<OrderItemEvent> items,
        BigDecimal totalAmount
) implements OrderEvent {
}
