package com.ivankos.inventoryservice.adapter.out.persistence;

import com.ivankos.inventoryservice.application.port.out.InventoryRepository;
import com.ivankos.inventoryservice.domain.InventoryItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
class JpaInventoryRepository implements InventoryRepository {

    private final SpringDataInventoryRepository springDataInventoryRepository;

    @Override
    public List<InventoryItem> findAllByProductId(Collection<UUID> productIds) {
        return springDataInventoryRepository.findAllById(productIds).stream()
                .map(this::toDomain)
                .toList();
    }

    // Must run inside the caller's transaction: relies on the first-level cache
    // returning the same managed instances (and their versions) loaded earlier.
    // A separate transaction would re-read fresh versions and silently lose updates.
    @Override
    public void saveAll(Collection<InventoryItem> items) {
        var productIdToItem = items.stream()
                .collect(Collectors.toMap(InventoryItem::getProductId, Function.identity()));

        springDataInventoryRepository.findAllById(productIdToItem.keySet()).forEach((jpaEntity -> {
            var item = productIdToItem.get(jpaEntity.getProductId());
            jpaEntity.setAvailable(item.getAvailable());
            jpaEntity.setReserved(item.getReserved());
        }));
    }

    private InventoryItem toDomain(InventoryItemJpaEntity inventoryItemEntity) {
        return InventoryItem.of(
                inventoryItemEntity.getProductId(),
                inventoryItemEntity.getAvailable(),
                inventoryItemEntity.getReserved());
    }
}
