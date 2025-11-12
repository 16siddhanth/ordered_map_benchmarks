package org.example.orderedmap.stm.library;

import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.example.orderedmap.api.MapMetrics;
import org.example.orderedmap.api.OrderedMap;
import static org.multiverse.api.StmUtils.atomic;
import static org.multiverse.api.StmUtils.newTxnRef;
import org.multiverse.api.callables.TxnCallable;
import org.multiverse.api.references.TxnRef;

/**
 * Ordered map backed by the Multiverse Gamma STM runtime.
 */
public class LibraryStmOrderedMap<K, V> implements OrderedMap<K, V> {

    private final ConcurrentSkipListMap<K, TxnRef<V>> store = new ConcurrentSkipListMap<>();

    @Override
    public V get(K key) {
        return runAtomic(txn -> {
            TxnRef<V> ref = store.get(key);
            if (ref == null) {
                return null;
            }
            return ref.get(txn);
        });
    }

    @Override
    public V put(K key, V value) {
        return runAtomic(txn -> {
            TxnRef<V> ref = ensureRef(key);
            V previous = ref.get(txn);
            ref.set(txn, value);
            return previous;
        });
    }

    @Override
    public V remove(K key) {
        return runAtomic(txn -> {
            TxnRef<V> ref = store.get(key);
            if (ref == null) {
                return null;
            }
            V previous = ref.get(txn);
            ref.set(txn, null);
            return previous;
        });
    }

    @Override
    public NavigableMap<K, V> rangeQuery(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
        return runAtomic(txn -> {
            TreeMap<K, V> snapshot = new TreeMap<>();
            NavigableMap<K, TxnRef<V>> view = selectRange(fromKey, fromInclusive, toKey, toInclusive);
            for (var entry : view.entrySet()) {
                V value = entry.getValue().get(txn);
                if (value != null) {
                    snapshot.put(entry.getKey(), value);
                }
            }
            return snapshot;
        });
    }

    @Override
    public int size() {
        return runAtomic(txn -> {
            int count = 0;
            for (TxnRef<V> ref : store.values()) {
                V value = ref.get(txn);
                if (value != null) {
                    count++;
                }
            }
            return count;
        });
    }

    @Override
    public void clear() {
        runAtomic(txn -> {
            for (TxnRef<V> ref : store.values()) {
                ref.set(txn, null);
            }
            return null;
        });
        store.clear();
    }

    @Override
    public MapMetrics snapshotMetrics() {
        return MapMetrics.empty();
    }

    @Override
    public void close() {
        store.clear();
    }

    private TxnRef<V> ensureRef(K key) {
        TxnRef<V> existing = store.get(key);
        if (existing != null) {
            return existing;
        }
        TxnRef<V> created = newTxnRef(null);
        TxnRef<V> race = store.putIfAbsent(key, created);
        return race != null ? race : created;
    }

    private NavigableMap<K, TxnRef<V>> selectRange(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
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

    private <T> T runAtomic(TxnCallable<T> callable) {
        return atomic(callable);
    }
}
