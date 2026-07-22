package com.ivankos.inventoryservice.adapter.in.kafka.event;

import java.util.List;
import java.util.UUID;

public record OrderCreatedEvent(UUID orderId, List<OrderItemEvent> items) {
}
