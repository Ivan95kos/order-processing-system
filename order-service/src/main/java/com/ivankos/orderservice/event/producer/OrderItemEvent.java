package com.ivankos.orderservice.event.producer;

import java.util.UUID;

public record OrderItemEvent(UUID productId, Integer quantity) {
}
