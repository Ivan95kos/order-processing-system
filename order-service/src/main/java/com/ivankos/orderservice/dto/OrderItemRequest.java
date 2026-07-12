package com.ivankos.orderservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record OrderItemRequest(@NotNull UUID productId, @NotNull @Positive Integer quantity) {
}
