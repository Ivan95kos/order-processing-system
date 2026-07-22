package com.ivankos.inventoryservice.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataInventoryRepository extends JpaRepository<InventoryItemJpaEntity, UUID> {
}
