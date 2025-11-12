package org.example.orderedmap;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.example.orderedmap.api.OrderedMap;
import org.example.orderedmap.locking.GlobalLockOrderedMap;
import org.example.orderedmap.locking.ShardedOrderedMap;
import org.example.orderedmap.skiplist.SkipListOrderedMap;
import org.example.orderedmap.stm.library.LibraryStmOrderedMap;
import org.example.orderedmap.stm.tiny.TinyStmOrderedMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class OrderedMapImplementationsTest {

    private final List<Supplier<OrderedMap<Integer, Integer>>> factories = List.of(
            GlobalLockOrderedMap::new,
            ShardedOrderedMap::new,
            SkipListOrderedMap::new,
            TinyStmOrderedMap::new,
            LibraryStmOrderedMap::new
    );

    @Test
    void putGetRemoveContract() {
        for (Supplier<OrderedMap<Integer, Integer>> factory : factories) {
            try (OrderedMap<Integer, Integer> map = factory.get()) {
                assertNull(map.get(42));
                assertNull(map.put(42, 100));
                assertEquals(100, map.get(42));
                assertEquals(100, map.put(42, 200));
                assertEquals(200, map.remove(42));
                assertNull(map.get(42));
            }
        }
    }

    @Test
    void rangeQueryReturnsSortedSnapshot() {
        for (Supplier<OrderedMap<Integer, Integer>> factory : factories) {
            try (OrderedMap<Integer, Integer> map = factory.get()) {
                for (int i = 0; i < 10; i++) {
                    map.put(i, i * 10);
                }
                var sub = map.rangeQuery(3, true, 6, true);
                assertEquals(4, sub.size());
                assertEquals(30, sub.get(3));
                assertEquals(60, sub.get(6));
            }
        }
    }

    @Test
    void concurrentWritesMaintainSize() throws Exception {
        for (Supplier<OrderedMap<Integer, Integer>> factory : factories) {
            try (OrderedMap<Integer, Integer> map = factory.get()) {
                ExecutorService pool = Executors.newFixedThreadPool(4);
                List<Callable<Void>> tasks = java.util.stream.IntStream.range(0, 4)
                        .<Callable<Void>>mapToObj(worker -> () -> {
                            int base = worker * 1000;
                            for (int i = 0; i < 250; i++) {
                                map.put(base + i, base + i);
                            }
                            return null;
                        })
                        .toList();
                pool.invokeAll(tasks);
                pool.shutdown();
                assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
                assertEquals(1000, map.size());
            }
        }
    }

    @Test
    void metricsSnapshotAvailable() {
        for (Supplier<OrderedMap<Integer, Integer>> factory : factories) {
            try (OrderedMap<Integer, Integer> map = factory.get()) {
                map.put(1, 1);
                assertNotNull(map.snapshotMetrics());
            }
        }
    }
}
