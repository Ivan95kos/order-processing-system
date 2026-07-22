package com.ivankos.inventoryservice.application;

import com.ivankos.inventoryservice.application.port.in.dto.ReserveItemRequest;
import com.ivankos.inventoryservice.application.port.in.dto.ReserveOrderRequest;
import com.ivankos.inventoryservice.application.port.out.InventoryEventPublisher;
import com.ivankos.inventoryservice.application.port.out.InventoryRepository;
import com.ivankos.inventoryservice.application.port.out.event.InventoryEvent;
import com.ivankos.inventoryservice.application.port.out.event.InventoryStatus;
import com.ivankos.inventoryservice.application.port.out.event.StockReservationFailedEvent;
import com.ivankos.inventoryservice.application.port.out.event.StockReservedEvent;
import com.ivankos.inventoryservice.domain.InventoryItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;

@ExtendWith(MockitoExtension.class)
class ReserveStockServiceTest {

    @Mock
    private InventoryRepository repository;

    @Mock
    private InventoryEventPublisher publisher;

    @Mock
    private TransactionTemplate transactionTemplate;

    private ReserveStockService reserveStockService;

    @BeforeEach
    void setUp() {
        reserveStockService = new ReserveStockService(repository, publisher, transactionTemplate);

        willAnswer(invocation -> {
            Consumer<TransactionStatus> action = invocation.getArgument(0);
            action.accept(null);
            return null;
        }).given(transactionTemplate).executeWithoutResult(any());
    }

    @Test
    void reserve_publishesStockReservedEvent_whenAllItemsAreAvailable() {
        // Given
        var productId = UUID.randomUUID();
        var orderId = UUID.randomUUID();
        var request = new ReserveOrderRequest(orderId, List.of(new ReserveItemRequest(productId, 3)));
        var item = InventoryItem.of(productId, 10, 0);

        given(repository.findAllByProductId(any())).willReturn(List.of(item));

        // When
        reserveStockService.reserve(request);

        // Then
        var eventCaptor = ArgumentCaptor.forClass(InventoryEvent.class);
        then(publisher).should().publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(StockReservedEvent.class);
        assertThat(eventCaptor.getValue().orderId()).isEqualTo(orderId);
        assertThat(eventCaptor.getValue().inventoryStatus()).isEqualTo(InventoryStatus.RESERVED);
        assertThat(item.getAvailable()).isEqualTo(7);
        assertThat(item.getReserved()).isEqualTo(3);
        then(repository).should().saveAll(List.of(item));
    }

    @Test
    void reserve_publishesStockReservationFailedEvent_whenProductIsNotFound() {
        // Given
        var productId = UUID.randomUUID();
        var orderId = UUID.randomUUID();
        var request = new ReserveOrderRequest(orderId, List.of(new ReserveItemRequest(productId, 3)));

        given(repository.findAllByProductId(any())).willReturn(List.of());

        // When
        reserveStockService.reserve(request);

        // Then
        var eventCaptor = ArgumentCaptor.forClass(InventoryEvent.class);
        then(publisher).should().publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(StockReservationFailedEvent.class);
        assertThat(eventCaptor.getValue().orderId()).isEqualTo(orderId);
        assertThat(eventCaptor.getValue().inventoryStatus()).isEqualTo(InventoryStatus.FAILED);
        then(repository).should(never()).saveAll(any());
    }

    @Test
    void reserve_publishesStockReservationFailedEvent_whenStockIsInsufficient() {
        // Given
        var productId = UUID.randomUUID();
        var orderId = UUID.randomUUID();
        var request = new ReserveOrderRequest(orderId, List.of(new ReserveItemRequest(productId, 11)));
        var item = InventoryItem.of(productId, 10, 0);

        given(repository.findAllByProductId(any())).willReturn(List.of(item));

        // When
        reserveStockService.reserve(request);

        // Then
        var eventCaptor = ArgumentCaptor.forClass(InventoryEvent.class);
        then(publisher).should().publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(StockReservationFailedEvent.class);
        assertThat(eventCaptor.getValue().orderId()).isEqualTo(orderId);
        then(repository).should(never()).saveAll(any());
    }

    @Test
    void reserve_savesNothing_whenOneOfSeveralItemsIsInsufficient() {
        // Given
        var okProductId1 = UUID.randomUUID();
        var okProductId2 = UUID.randomUUID();
        var badProductId = UUID.randomUUID();
        var orderId = UUID.randomUUID();
        var request = new ReserveOrderRequest(orderId, List.of(
                new ReserveItemRequest(okProductId1, 3),
                new ReserveItemRequest(okProductId2, 3),
                new ReserveItemRequest(badProductId, 3)));
        var ok1 = InventoryItem.of(okProductId1, 10, 0);
        var ok2 = InventoryItem.of(okProductId2, 10, 0);
        var bad = InventoryItem.of(badProductId, 1, 0);

        given(repository.findAllByProductId(any())).willReturn(List.of(ok1, ok2, bad));

        // When
        reserveStockService.reserve(request);

        // Then
        var eventCaptor = ArgumentCaptor.forClass(InventoryEvent.class);
        then(publisher).should().publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(StockReservationFailedEvent.class);
        assertThat(eventCaptor.getValue().orderId()).isEqualTo(orderId);
        then(repository).should(never()).saveAll(any());
    }
}
