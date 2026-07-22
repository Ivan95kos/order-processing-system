package com.ivankos.inventoryservice.application.port.out.event;

import java.time.Instant;
import java.util.UUID;

public record StockReservationFailedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID orderId,
        InventoryStatus inventoryStatus
) implements InventoryEvent {

    public StockReservationFailedEvent(
            UUID eventId,
            Instant occurredAt,
            UUID orderId) {
        this(eventId, occurredAt, orderId, InventoryStatus.FAILED);

    }

}
