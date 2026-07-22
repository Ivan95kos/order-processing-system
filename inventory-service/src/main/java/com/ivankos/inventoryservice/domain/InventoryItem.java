package com.ivankos.inventoryservice.domain;

import com.ivankos.inventoryservice.domain.exception.IllegalReleaseException;
import com.ivankos.inventoryservice.domain.exception.InsufficientStockException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class InventoryItem {

    private final UUID productId;
    private Integer available;
    private Integer reserved;

    public static InventoryItem of(UUID productId, Integer available, Integer reserved) {
        return new InventoryItem(productId, available, reserved);
    }

    public void reserve(Integer quantity) {
        if (available < quantity) {
            throw new InsufficientStockException(
                    "Product %s has the %s available items, but requested %s".formatted(productId, available, quantity));
        }
        available -= quantity;
        reserved += quantity;
    }

    public void release(Integer quantity) {
        if (reserved < quantity) {
            throw new IllegalReleaseException(
                    "Product %s has the %s reserved items, but returned %s".formatted(productId, reserved, quantity));
        }
        available += quantity;
        reserved -= quantity;
    }

}
