package com.ivankos.inventoryservice.adapter.out.persistence;

import com.ivankos.inventoryservice.application.port.out.InventoryRepository;
import com.ivankos.inventoryservice.domain.InventoryItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
class JpaInventoryRepository implements InventoryRepository {

    private final SpringDataInventoryRepository springDataInventoryRepository;

    @Override
    public Optional<InventoryItem> findByProductId(UUID productId) {
        return springDataInventoryRepository.findById(productId)
                .map(this::toDomain);
    }

    @Override
    public List<InventoryItem> findAllByProductId(Collection<UUID> productIds) {
        return springDataInventoryRepository.findAllById(productIds).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void save(InventoryItem inventoryItem) {
        springDataInventoryRepository.save(toEntity(inventoryItem));
    }

    @Override
    @Transactional
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

    private InventoryItemJpaEntity toEntity(InventoryItem inventoryItemDomain) {
        return InventoryItemJpaEntity.create(
                inventoryItemDomain.getProductId(),
                inventoryItemDomain.getAvailable(),
                inventoryItemDomain.getReserved());
    }
}
