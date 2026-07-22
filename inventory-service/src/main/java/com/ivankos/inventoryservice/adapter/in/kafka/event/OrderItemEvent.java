package com.ivankos.inventoryservice.adapter.in.kafka.event;

import java.util.UUID;

public record OrderItemEvent(UUID productId, Integer quantity) {
}
