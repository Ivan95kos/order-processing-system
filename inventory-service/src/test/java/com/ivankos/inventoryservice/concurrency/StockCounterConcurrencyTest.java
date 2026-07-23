package com.ivankos.inventoryservice.concurrency;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Teaching artifact — no Spring, no database. Shows why a {@link ConcurrentHashMap} being
 * thread-safe per operation does NOT make a get / check / put sequence safe. Three paths:
 * <ul>
 *   <li>{@code reserveBroken} — get/check/put as three ops: loses updates.</li>
 *   <li>{@code reserveAtomic} — one atomic {@link ConcurrentHashMap#compute}: correct.</li>
 *   <li>{@code reserveSynchronized} — the same three ops under the instance monitor: also correct.</li>
 * </ul>
 * <p>
 * <b>If atomic and synchronized give the same answer, why prefer compute()? Lock granularity.</b>
 * {@code synchronized} takes one lock over the <i>whole</i> counter, so a reservation of product A
 * blocks a reservation of product B even though they touch different keys — every writer serializes
 * on a single monitor. {@code compute} locks only the bin (bucket) that holds that one key: in the
 * Java 8+ CHM each bin is guarded by the {@code synchronized} on its first node, so writes to
 * different keys (different bins) proceed in parallel, and reads are lock-free. Under the one-hot-key
 * contention of this test the two are indistinguishable; on a real map spread across many products
 * the coarse lock throttles throughput to a single writer while {@code compute} scales with the
 * number of bins. (This is also why {@code reserveSynchronized} no longer needs a ConcurrentHashMap
 * at all — a plain HashMap under the same monitor would be just as safe, and just as coarse.)
 * <p>
 * This is the in-memory shadow of the real optimistic-lock-plus-retry guard in
 * {@code ReserveStockService}: same hazard (lost update under concurrency), stripped of Postgres.
 * <p>
 * On the broken path a lost update is overwhelmingly likely under this contention but, being a race,
 * never strictly guaranteed — so the only assertion is {@code finalStock > 0}. That the exact value
 * differs run to run is the nature of a data race: we log the sampled remainders rather than assert
 * on them, because non-determinism can be observed but not pinned down by a test.
 */
class StockCounterConcurrencyTest {

    private static final Logger log = LoggerFactory.getLogger(StockCounterConcurrencyTest.class);

    private static final UUID PRODUCT = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final int INITIAL_STOCK = 1000;
    private static final int THREADS = 100;
    private static final int RESERVES_PER_THREAD = 10; // 100 * 10 = 1000 reservations of one unit each

    @Test
    void reserveBroken_losesUpdatesAndLeavesStockAboveZero_whenThreadsRaceOnOneKey() throws InterruptedException {
        // Given/When: run the racy get/check/put a handful of times, collecting each final stock
        Set<Integer> observedRemainders = new HashSet<>();
        for (int run = 0; run < 5; run++) {
            observedRemainders.add(finalStockAfterConcurrentReserves(counter -> counter.reserveBroken(PRODUCT, 1)));
        }

        // Then: updates were lost every time — never drained to zero.
        assertThat(observedRemainders).allSatisfy(remainder -> assertThat(remainder).isPositive());

        // The run-to-run spread is the real lesson, but it is observed, not asserted: a race has no
        // single reproducible answer to pin a test to.
        log.info("reserveBroken left these remainders across runs: {}", observedRemainders);
    }

    @Test
    void reserveAtomic_drainsStockToExactlyZero_whenThreadsRaceOnOneKey() throws InterruptedException {
        // Given/When/Then: the atomic compute counts every single reservation, on every run
        for (int run = 0; run < 3; run++) {
            int remainder = finalStockAfterConcurrentReserves(counter -> counter.reserveAtomic(PRODUCT, 1));
            assertThat(remainder).isZero();
        }
    }

    @Test
    void reserveSynchronized_drainsStockToExactlyZero_whenThreadsRaceOnOneKey() throws InterruptedException {
        // Given/When/Then: guarding the whole read-modify-write with the monitor is just as correct
        // as compute() — same result, coarser lock (see class Javadoc).
        for (int run = 0; run < 3; run++) {
            int remainder = finalStockAfterConcurrentReserves(counter -> counter.reserveSynchronized(PRODUCT, 1));
            assertThat(remainder).isZero();
        }
    }

    @Test
    void reserveAtomic_leavesStockUnchanged_whenTheComputeLambdaThrows() {
        // Given: fewer units than the reservation asks for
        var counter = new StockCounter(PRODUCT, 5);

        // When: the compute lambda throws in the middle of the read-modify-write
        assertThatThrownBy(() -> counter.reserveAtomic(PRODUCT, 6))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("insufficient stock");

        // Then: a throwing remapping function makes compute() write nothing, so the balance is
        // exactly what it was — not a half-applied value. The lock is released in compute()'s own
        // finally, leaving the map consistent.
        assertThat(counter.available(PRODUCT)).isEqualTo(5);
    }

    /**
     * Fires {@link #THREADS} threads, each reserving one unit {@link #RESERVES_PER_THREAD} times,
     * all released together via the latch so they genuinely contend. Returns the leftover stock.
     */
    private int finalStockAfterConcurrentReserves(Consumer<StockCounter> reserveOneUnit) throws InterruptedException {
        var counter = new StockCounter(PRODUCT, INITIAL_STOCK);
        var start = new CountDownLatch(1);
        var done = new CountDownLatch(THREADS);
        var failure = new AtomicReference<Throwable>();

        // Virtual threads over a platform pool — the point is footprint, not speed. The real cost of
        // a platform thread isn't its stack (that's reserved address space, committed page by page as
        // used) but that it's an OS kernel object: creating one is a syscall and switching between
        // them goes through the OS scheduler. A virtual thread keeps its stack on the heap and
        // switches in user space, which is why it's cheap. (Not something to *measure* on a suite this
        // small — JVM warmup and class loading dwarf the microseconds of real work.)
        //
        // Caveat this very test triggers: reserveSynchronized has 100 threads on one monitor, and on
        // Java 21 a virtual thread blocked in `synchronized` pins its carrier. With carriers defaulting
        // to one-per-core, the scheduler compensates by growing more (up to 256) — i.e. platform
        // threads back through the side door. It stays correct only because of that compensation; the
        // block being tiny doesn't help, what matters is how many threads stack up on it.
        // compute()/ReentrantLock would not pin (monitor pinning removed by JEP 491 in JDK 24+).
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int t = 0; t < THREADS; t++) {
                pool.submit(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < RESERVES_PER_THREAD; i++) {
                            reserveOneUnit.accept(counter);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (Throwable t2) {
                        failure.compareAndSet(null, t2);
                    } finally {
                        done.countDown();
                    }
                });
            }

            start.countDown(); // all threads leap at once
            assertThat(done.await(30, TimeUnit.SECONDS)).as("all reservation threads finished").isTrue();
        }

        assertThat(failure.get()).as("no reservation thread threw").isNull();
        return counter.available(PRODUCT);
    }

    /**
     * Minimal stand-in for the inventory store: one key, an int balance. {@code reserveBroken} is
     * the bug (three separate map operations); {@code reserveAtomic} (fine-grained) and
     * {@code reserveSynchronized} (coarse) are the two correct fixes.
     */
    static class StockCounter {

        private final Map<UUID, Integer> stock = new ConcurrentHashMap<>();

        StockCounter(UUID productId, int initial) {
            stock.put(productId, initial);
        }

        // BROKEN: get / check / put — three separate operations, so another thread can slip a write
        // between the read and the put, and that other thread's decrement is silently overwritten.
        void reserveBroken(UUID productId, int qty) {
            Integer available = stock.get(productId);
            if (available != null && available >= qty) {
                stock.put(productId, available - qty);
            }
        }

        // FIXED: atomic read-modify-write on the single key — the map holds the bin lock for the key
        // across the whole lambda, so no other thread can interleave.
        void reserveAtomic(UUID productId, int qty) {
            stock.compute(productId, (id, available) -> {
                if (available == null || available < qty) {
                    throw new IllegalStateException("insufficient stock");
                }
                return available - qty;
            });
        }

        // ALSO CORRECT, but coarser: the same racy get/check/put made safe by holding the instance
        // monitor across all three steps. Correctness-wise identical to reserveAtomic; the lock is
        // just far bigger (see the class Javadoc on granularity). Note the ConcurrentHashMap is now
        // pointless here — a plain HashMap would be equally safe, since every access is serialized.
        synchronized void reserveSynchronized(UUID productId, int qty) {
            Integer available = stock.get(productId);
            if (available != null && available >= qty) {
                stock.put(productId, available - qty);
            }
        }

        int available(UUID productId) {
            return stock.getOrDefault(productId, 0);
        }
    }
}
