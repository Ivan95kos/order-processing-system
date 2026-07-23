package com.ivankos.inventoryservice.application.port.out;


import com.ivankos.inventoryservice.domain.InventoryItem;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface InventoryRepository {
    List<InventoryItem> findAllByProductId(Collection<UUID> productIds);

    void saveAll(Collection<InventoryItem> items);
}
