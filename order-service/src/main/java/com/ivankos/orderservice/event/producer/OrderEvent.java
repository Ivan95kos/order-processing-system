package com.ivankos.orderservice.event.producer;

import java.time.Instant;
import java.util.UUID;

public sealed interface OrderEvent permits OrderCreatedEvent {
    UUID eventId();
    Instant occurredAt();
    OrderEventType eventType();
    UUID orderId();
}
