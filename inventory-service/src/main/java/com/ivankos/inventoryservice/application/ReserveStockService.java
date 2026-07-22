package com.ivankos.inventoryservice.application;

import com.ivankos.inventoryservice.application.port.in.ReserveStockUseCase;
import com.ivankos.inventoryservice.application.port.in.dto.ReserveItemRequest;
import com.ivankos.inventoryservice.application.port.in.dto.ReserveOrderRequest;
import com.ivankos.inventoryservice.application.port.out.InventoryEventPublisher;
import com.ivankos.inventoryservice.application.port.out.InventoryRepository;
import com.ivankos.inventoryservice.application.port.out.event.StockReservationFailedEvent;
import com.ivankos.inventoryservice.application.port.out.event.StockReservedEvent;
import com.ivankos.inventoryservice.domain.exception.InsufficientStockException;
import com.ivankos.inventoryservice.domain.exception.ProductNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReserveStockService implements ReserveStockUseCase {

    private final InventoryRepository repository;
    private final InventoryEventPublisher publisher;
    private final TransactionTemplate transactionTemplate;

    @Override
    public void reserve(ReserveOrderRequest reservationRequest) {

        var productIdToQuantity = reservationRequest.items().stream()
                .collect(Collectors.toMap(ReserveItemRequest::productId, ReserveItemRequest::quantity));

        try {
            transactionTemplate.executeWithoutResult(status -> reserveAndSave(productIdToQuantity));
            publisher.publish(new StockReservedEvent(UUID.randomUUID(), Instant.now(), reservationRequest.orderId()));
            log.info("Stock reserved for orderId {}, products {}", reservationRequest.orderId(), productIdToQuantity.keySet());
        } catch (ProductNotFoundException | InsufficientStockException e) {
            log.warn("Stock reservation failed for orderId {}: {}", reservationRequest.orderId(), e.getMessage());
            publisher.publish(new StockReservationFailedEvent(UUID.randomUUID(), Instant.now(), reservationRequest.orderId()));
        }
    }

    private void reserveAndSave(Map<UUID, Integer> productIdToQuantity) {
        var items = repository.findAllByProductId(productIdToQuantity.keySet());

        if (items.isEmpty() || productIdToQuantity.size() != items.size()) {
            throw new ProductNotFoundException("Product not found by ids: " + productIdToQuantity.keySet());
        }

        items.forEach(item -> item.reserve(productIdToQuantity.get(item.getProductId())));
        repository.saveAll(items);
    }
}
