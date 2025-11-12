package org.example.orderedmap.locking;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.example.orderedmap.api.OrderedMap;

/**
 * Fine-grained implementation that shards a {@link TreeMap} across multiple locks based on key hash.
 */
public class ShardedOrderedMap<K, V> implements OrderedMap<K, V> {

    private final List<Shard<K, V>> shards;
    private final int mask;

    public ShardedOrderedMap(int shardCount) {
        if (Integer.bitCount(shardCount) != 1) {
            throw new IllegalArgumentException("shardCount must be a power-of-two value");
        }
        this.mask = shardCount - 1;
        this.shards = new ArrayList<>(shardCount);
        for (int i = 0; i < shardCount; i++) {
            shards.add(new Shard<>());
        }
    }

    public ShardedOrderedMap() {
        this(16);
    }

    @Override
    public V get(K key) {
        Shard<K, V> shard = shardFor(key);
        shard.lock.readLock().lock();
        try {
            return shard.map.get(key);
        } finally {
            shard.lock.readLock().unlock();
        }
    }

    @Override
    public V put(K key, V value) {
        Objects.requireNonNull(key, "key");
        Shard<K, V> shard = shardFor(key);
        shard.lock.writeLock().lock();
        try {
            return shard.map.put(key, value);
        } finally {
            shard.lock.writeLock().unlock();
        }
    }

    @Override
    public V remove(K key) {
        Shard<K, V> shard = shardFor(key);
        shard.lock.writeLock().lock();
        try {
            return shard.map.remove(key);
        } finally {
            shard.lock.writeLock().unlock();
        }
    }

    @Override
    public NavigableMap<K, V> rangeQuery(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
        List<Shard<K, V>> locks = this.shards;
        for (Shard<K, V> shard : locks) {
            shard.lock.readLock().lock();
        }
        try {
            TreeMap<K, V> result = new TreeMap<>();
            for (Shard<K, V> shard : locks) {
                NavigableMap<K, V> view = slice(shard.map, fromKey, fromInclusive, toKey, toInclusive);
                result.putAll(view);
            }
            return result;
        } finally {
            for (int i = locks.size() - 1; i >= 0; i--) {
                locks.get(i).lock.readLock().unlock();
            }
        }
    }

    @Override
    public int size() {
        int total = 0;
        for (Shard<K, V> shard : shards) {
            shard.lock.readLock().lock();
        }
        try {
            for (Shard<K, V> shard : shards) {
                total += shard.map.size();
            }
        } finally {
            for (int i = shards.size() - 1; i >= 0; i--) {
                shards.get(i).lock.readLock().unlock();
            }
        }
        return total;
    }

    @Override
    public void clear() {
        for (Shard<K, V> shard : shards) {
            shard.lock.writeLock().lock();
        }
        try {
            for (Shard<K, V> shard : shards) {
                shard.map.clear();
            }
        } finally {
            for (int i = shards.size() - 1; i >= 0; i--) {
                shards.get(i).lock.writeLock().unlock();
            }
        }
    }

    private NavigableMap<K, V> slice(NavigableMap<K, V> map, K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
        if (fromKey == null && toKey == null) {
            return new TreeMap<>(map);
        }
        if (fromKey == null) {
            return new TreeMap<>(map.headMap(toKey, toInclusive));
        }
        if (toKey == null) {
            return new TreeMap<>(map.tailMap(fromKey, fromInclusive));
        }
        return new TreeMap<>(map.subMap(fromKey, fromInclusive, toKey, toInclusive));
    }

    private Shard<K, V> shardFor(K key) {
        Objects.requireNonNull(key, "key");
        int hash = key.hashCode();
        int index = smear(hash) & mask;
        return shards.get(index);
    }

    private int smear(int hashCode) {
        int h = hashCode;
        h ^= (h >>> 20) ^ (h >>> 12);
        return h ^ (h >>> 7) ^ (h >>> 4);
    }

    private static final class Shard<K, V> {
        private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        private final TreeMap<K, V> map = new TreeMap<>();
    }
}
