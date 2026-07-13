package com.ivankos.orderservice.event;

import java.util.UUID;

public record OrderItemEvent(UUID productId, Integer quantity) {
}
