package com.ivankos.inventoryservice.application.port.in.dto;

import java.util.List;
import java.util.UUID;

public record ReserveOrderRequest(UUID orderId, List<ReserveItemRequest> items) {
}
