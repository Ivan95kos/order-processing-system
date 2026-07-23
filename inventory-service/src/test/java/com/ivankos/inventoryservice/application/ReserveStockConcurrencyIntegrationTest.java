package com.ivankos.inventoryservice.application;

import com.ivankos.inventoryservice.IntegrationTestSupport;
import com.ivankos.inventoryservice.adapter.out.persistence.SpringDataInventoryRepository;
import com.ivankos.inventoryservice.application.port.in.dto.ReserveItemRequest;
import com.ivankos.inventoryservice.application.port.in.dto.ReserveOrderRequest;
import com.ivankos.inventoryservice.application.port.out.InventoryEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Honest concurrency test: a real Postgres (Testcontainers, via {@link IntegrationTestSupport}) and
 * two threads calling {@link ReserveStockService#reserve} against the same product at the same
 * instant, so the optimistic-lock-plus-retry path is genuinely exercised rather than mocked away.
 * <p>
 * The Kafka listener is off (from the base class) and the outbound {@link InventoryEventPublisher}
 * is stubbed: the contract under test is the DB concurrency guard, not the broker.
 * <p>
 * What this does NOT assert: that a version conflict actually fired on any given run. With two
 * threads the collision is likely but not guaranteed, and retry exhaustion is not forced. The
 * real assertion is the invariant {@code available + reserved == initial} — a lost update (the
 * bug optimistic locking exists to prevent) is the only thing that breaks it.
 */
class ReserveStockConcurrencyIntegrationTest extends IntegrationTestSupport {

    // Seeded by changeset 002 with available=100, reserved=0 (version defaults to 0 via changeset 003).
    private static final UUID SEEDED_PRODUCT = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final int INITIAL_AVAILABLE = 100;

    @Autowired
    private ReserveStockService reserveStockService;

    @Autowired
    private SpringDataInventoryRepository springDataInventoryRepository;

    @MockitoBean
    private InventoryEventPublisher publisher;

    @Test
    void reserve_neverLosesAnUpdate_whenTwoOrdersHitTheSameProductAtOnce() throws InterruptedException {
        // Given: two orders reserving the same product, wired to start on one signal
        int firstQuantity = 3;
        int secondQuantity = 5;

        var start = new CountDownLatch(1);
        var done = new CountDownLatch(2);
        var failure = new AtomicReference<Throwable>();

        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            pool.submit(reservationTask(orderReserving(firstQuantity), start, done, failure));
            pool.submit(reservationTask(orderReserving(secondQuantity), start, done, failure));

            // When: both threads are unleashed in the same instant so they collide on the version
            start.countDown();
            boolean finished = done.await(30, TimeUnit.SECONDS);

            // Then: both completed, neither thread blew up
            assertThat(finished).as("both reservations completed in time").isTrue();
            assertThat(failure.get()).as("no reservation thread failed").isNull();
        }

        // And: nothing vanished — both reservations landed on top of each other, not over each other
        var item = springDataInventoryRepository.findById(SEEDED_PRODUCT).orElseThrow();
        assertThat(item.getReserved()).isEqualTo(firstQuantity + secondQuantity);
        assertThat(item.getAvailable()).isEqualTo(INITIAL_AVAILABLE - firstQuantity - secondQuantity);
        assertThat(item.getAvailable() + item.getReserved()).isEqualTo(INITIAL_AVAILABLE);
    }

    private ReserveOrderRequest orderReserving(int quantity) {
        return new ReserveOrderRequest(UUID.randomUUID(), List.of(new ReserveItemRequest(SEEDED_PRODUCT, quantity)));
    }

    private Runnable reservationTask(ReserveOrderRequest request, CountDownLatch start, CountDownLatch done,
                                     AtomicReference<Throwable> failure) {
        return () -> {
            try {
                start.await();
                reserveStockService.reserve(request);
            } catch (Throwable t) {
                failure.compareAndSet(null, t);
            } finally {
                done.countDown();
            }
        };
    }
}
