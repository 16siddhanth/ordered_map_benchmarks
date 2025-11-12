package org.example.orderedmap.stm.tiny;

import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.example.orderedmap.api.MapMetrics;
import org.example.orderedmap.api.OrderedMap;

/**
 * Ordered map backed by {@link TinyStm} runtime for optimistic concurrency.
 */
public class TinyStmOrderedMap<K, V> implements OrderedMap<K, V> {

    private final TinyStm stm = new TinyStm();
    private final ConcurrentSkipListMap<K, TinyStmRef<V>> store = new ConcurrentSkipListMap<>();

    @Override
    public V get(K key) {
        return stm.execute(tx -> {
            TinyStmRef<V> ref = store.get(key);
            if (ref == null) {
                return null;
            }
            return tx.read(ref);
        });
    }

    @Override
    public V put(K key, V value) {
        return stm.execute(tx -> {
            TinyStmRef<V> ref = store.computeIfAbsent(key, k -> new TinyStmRef<>(null, 0L));
            V previous = tx.read(ref);
            tx.write(ref, value);
            return previous;
        });
    }

    @Override
    public V remove(K key) {
        return stm.execute(tx -> {
            TinyStmRef<V> ref = store.get(key);
            if (ref == null) {
                return null;
            }
            V previous = tx.read(ref);
            tx.write(ref, null);
            return previous;
        });
    }

    @Override
    public NavigableMap<K, V> rangeQuery(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
        return stm.execute(tx -> {
            TreeMap<K, V> snapshot = new TreeMap<>();
            NavigableMap<K, TinyStmRef<V>> view = selectRange(fromKey, fromInclusive, toKey, toInclusive);
            for (var entry : view.entrySet()) {
                V value = tx.read(entry.getValue());
                if (value != null) {
                    snapshot.put(entry.getKey(), value);
                }
            }
            return snapshot;
        });
    }

    @Override
    public int size() {
        return stm.execute(tx -> {
            int count = 0;
            for (TinyStmRef<V> ref : store.values()) {
                V value = tx.read(ref);
                if (value != null) {
                    count++;
                }
            }
            return count;
        });
    }

    @Override
    public void clear() {
        stm.execute(tx -> {
            for (TinyStmRef<V> ref : store.values()) {
                tx.write(ref, null);
            }
            return null;
        });
        store.clear();
        stm.resetStats();
    }

    @Override
    public MapMetrics snapshotMetrics() {
        return new MapMetrics(stm.getCommitCount(), stm.getAbortCount(), stm.getMaxRetries());
    }

    private NavigableMap<K, TinyStmRef<V>> selectRange(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
        if (fromKey == null && toKey == null) {
            return store;
        }
        if (fromKey == null) {
            return store.headMap(toKey, toInclusive);
        }
        if (toKey == null) {
            return store.tailMap(fromKey, fromInclusive);
        }
        return store.subMap(fromKey, fromInclusive, toKey, toInclusive);
    }
}
