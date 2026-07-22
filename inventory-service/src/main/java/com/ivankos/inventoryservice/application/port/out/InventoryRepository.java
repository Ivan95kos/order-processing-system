package com.ivankos.inventoryservice.application.port.out;


import com.ivankos.inventoryservice.domain.InventoryItem;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository {
    Optional<InventoryItem> findByProductId(UUID productId);
    List<InventoryItem> findAllByProductId(Collection<UUID> productIds);

    void save(InventoryItem item);
    void saveAll(Collection<InventoryItem> items);
}
