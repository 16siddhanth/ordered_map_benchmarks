package org.example.orderedmap.locking;

import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.example.orderedmap.api.OrderedMap;

/**
 * Coarse-grained implementation that protects a {@link TreeMap} with a single read/write lock.
 */
public class GlobalLockOrderedMap<K, V> implements OrderedMap<K, V> {

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final TreeMap<K, V> delegate;

    public GlobalLockOrderedMap() {
        this.delegate = new TreeMap<>();
    }

    public GlobalLockOrderedMap(TreeMap<K, V> backing) {
        this.delegate = Objects.requireNonNull(backing, "backing");
    }

    @Override
    public V get(K key) {
        lock.readLock().lock();
        try {
            return delegate.get(key);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public V put(K key, V value) {
        lock.writeLock().lock();
        try {
            return delegate.put(key, value);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public V remove(K key) {
        lock.writeLock().lock();
        try {
            return delegate.remove(key);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public NavigableMap<K, V> rangeQuery(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
        lock.readLock().lock();
        try {
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
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int size() {
        lock.readLock().lock();
        try {
            return delegate.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            delegate.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }
}
