package com.ivankos.orderservice.dto;

import com.ivankos.orderservice.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID customerId,
        BigDecimal totalAmount,
        OrderStatus status,
        Instant createdAt) {
}
