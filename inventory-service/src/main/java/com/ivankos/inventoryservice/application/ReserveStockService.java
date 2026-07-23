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
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReserveStockService implements ReserveStockUseCase {

    private static final int MAX_ATTEMPTS = 3;
    private static final long BASE_BACKOFF_MILLIS = 50L;

    private final InventoryRepository inventoryRepository;
    private final InventoryEventPublisher publisher;
    private final TransactionTemplate transactionTemplate;

    @Override
    public void reserve(ReserveOrderRequest reservationRequest) {

        var productIdToQuantity = reservationRequest.items().stream()
                .collect(Collectors.toMap(ReserveItemRequest::productId, ReserveItemRequest::quantity, Integer::sum));

        try {
            reserveWithRetry(productIdToQuantity);
            publisher.publish(new StockReservedEvent(UUID.randomUUID(), Instant.now(), reservationRequest.orderId()));
            log.info("Stock reserved for orderId {}, products {}", reservationRequest.orderId(), productIdToQuantity.keySet());
        } catch (ProductNotFoundException | InsufficientStockException e) {
            log.warn("Stock reservation failed for orderId {}: {}", reservationRequest.orderId(), e.getMessage());
            publisher.publish(new StockReservationFailedEvent(UUID.randomUUID(), Instant.now(), reservationRequest.orderId()));
        }
    }

    private void reserveWithRetry(Map<UUID, Integer> productIdToQuantity) {
        for (int attempt = 1; ; attempt++) {
            try {
                transactionTemplate.executeWithoutResult(status -> reserveAndSave(productIdToQuantity));
                return;
            } catch (ObjectOptimisticLockingFailureException e) {
                if (attempt == MAX_ATTEMPTS) {
                    log.error("Giving up on stock reservation after {} attempts, optimistic lock conflict persists", MAX_ATTEMPTS, e);
                    throw e;   // technical failure -> let Kafka retry/DLQ handle it
                }
                log.warn("Optimistic lock conflict, attempt {}/{}", attempt, MAX_ATTEMPTS);
                sleepWithJitter(attempt);
            }
        }
    }

    private void reserveAndSave(Map<UUID, Integer> productIdToQuantity) {
        var items = inventoryRepository.findAllByProductId(productIdToQuantity.keySet());

        if (items.isEmpty() || productIdToQuantity.size() != items.size()) {
            throw new ProductNotFoundException("Product not found by ids: " + productIdToQuantity.keySet());
        }

        items.forEach(item -> item.reserve(productIdToQuantity.get(item.getProductId())));
        inventoryRepository.saveAll(items);
    }

    private void sleepWithJitter(int attempt) {
        long base = BASE_BACKOFF_MILLIS * attempt;
        long delay = ThreadLocalRandom.current().nextLong(base, base * 2);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while backing off before retry", e);
        }
    }
}
