package org.example.orderedmap.skiplist;

import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.example.orderedmap.api.OrderedMap;

/**
 * Wrapper around {@link ConcurrentSkipListMap} to conform to {@link OrderedMap}.
 */
public class SkipListOrderedMap<K, V> implements OrderedMap<K, V> {

    private final ConcurrentSkipListMap<K, V> delegate;

    public SkipListOrderedMap() {
        this.delegate = new ConcurrentSkipListMap<>();
    }

    @Override
    public V get(K key) {
        return delegate.get(key);
    }

    @Override
    public V put(K key, V value) {
        Objects.requireNonNull(key, "key");
        return delegate.put(key, value);
    }

    @Override
    public V remove(K key) {
        return delegate.remove(key);
    }

    @Override
    public NavigableMap<K, V> rangeQuery(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
        if (fromKey == null && toKey == null) {
            return new TreeMap<>(delegate);
        }
        if (fromKey == null) {
            return new TreeMap<>(delegate.headMap(toKey, toInclusive));
        }
        if (toKey == null) {
            return new TreeMap<>(delegate.tailMap(fromKey, fromInclusive));
        }
        return new TreeMap<>(delegate.subMap(fromKey, fromInclusive, toKey, toInclusive));
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public void clear() {
        delegate.clear();
    }
}
