package org.example.orderedmap.api;

import java.util.NavigableMap;

/**
 * Minimal abstraction for a thread-safe ordered map with range query support.
 */
public interface OrderedMap<K, V> extends AutoCloseable {

    V get(K key);

    V put(K key, V value);

    V remove(K key);

    NavigableMap<K, V> rangeQuery(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive);

    int size();

    void clear();

    default MapMetrics snapshotMetrics() {
        return MapMetrics.empty();
    }

    @Override
    default void close() {
        // default no-op
    }
}
