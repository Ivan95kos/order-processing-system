package com.ivankos.inventoryservice.application.port.out;

import com.ivankos.inventoryservice.application.port.out.event.InventoryEvent;

public interface InventoryEventPublisher {
    void publish(InventoryEvent event);
}
