package com.ivankos.inventoryservice.domain;

import com.ivankos.inventoryservice.domain.exception.IllegalReleaseException;
import com.ivankos.inventoryservice.domain.exception.InsufficientStockException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InventoryItemTest {

    private final UUID productId = UUID.randomUUID();

    @Test
    void reserve_reducesAvailableAndIncreasesReserved() {
        // Given
        var item = InventoryItem.of(productId, 10, 0);

        // When
        item.reserve(3);

        // Then
        assertThat(item.getAvailable()).isEqualTo(7);
        assertThat(item.getReserved()).isEqualTo(3);
    }

    @Test
    void reserve_succeedsWhenQuantityEqualsAvailable() {
        // Given
        var item = InventoryItem.of(productId, 10, 0);

        // When
        item.reserve(10);

        // Then
        assertThat(item.getAvailable()).isZero();
        assertThat(item.getReserved()).isEqualTo(10);
    }

    @Test
    void reserve_throwsWhenQuantityExceedsAvailable_andLeavesStateUnchanged() {
        // Given
        var item = InventoryItem.of(productId, 10, 0);

        // When
        // Then
        assertThatThrownBy(() -> item.reserve(11))
                .isInstanceOf(InsufficientStockException.class);

        assertThat(item.getAvailable()).isEqualTo(10);
        assertThat(item.getReserved()).isZero();
    }

    @Test
    void release_increasesAvailableAndReducesReserved() {
        // Given
        var item = InventoryItem.of(productId, 0, 5);

        // When
        item.release(3);

        // Then
        assertThat(item.getReserved()).isEqualTo(2);
        assertThat(item.getAvailable()).isEqualTo(3);
    }

    @Test
    void release_throwsWhenQuantityExceedsReserved_andLeavesStateUnchanged() {
        // Given
        var item = InventoryItem.of(productId, 0, 5);

        // When
        // Then
        assertThatThrownBy(() -> item.release(6))
                .isInstanceOf(IllegalReleaseException.class);

        assertThat(item.getReserved()).isEqualTo(5);
        assertThat(item.getAvailable()).isZero();
    }
}