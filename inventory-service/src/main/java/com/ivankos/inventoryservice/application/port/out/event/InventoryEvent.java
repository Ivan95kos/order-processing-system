package com.ivankos.inventoryservice.application.port.out.event;

import java.time.Instant;
import java.util.UUID;

public sealed interface InventoryEvent
        permits StockReservedEvent, StockReservationFailedEvent {
    UUID eventId();

    Instant occurredAt();

    UUID orderId();

    InventoryStatus inventoryStatus();   // ← дискримінатор
}
