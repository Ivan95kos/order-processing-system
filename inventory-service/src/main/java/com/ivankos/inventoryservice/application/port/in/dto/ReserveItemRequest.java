package com.ivankos.inventoryservice.application.port.in.dto;

import java.util.UUID;

public record ReserveItemRequest(UUID productId, Integer quantity) {
}
