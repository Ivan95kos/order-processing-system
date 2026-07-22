package com.ivankos.inventoryservice.application.port.out.event;

import java.time.Instant;
import java.util.UUID;

public record StockReservedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID orderId,
        InventoryStatus inventoryStatus
) implements InventoryEvent {

    public StockReservedEvent(
            UUID eventId,
            Instant occurredAt,
            UUID orderId) {
        this(eventId, occurredAt, orderId, InventoryStatus.RESERVED);

    }
}
