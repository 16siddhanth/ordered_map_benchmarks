package org.example.orderedmap.util;

import java.util.Arrays;

/**
 * Minimal dynamically sized array for long values without boxing overhead.
 */
public final class LongArray {

    private long[] values;
    private int size;

    public LongArray() {
        this(1024);
    }

    public LongArray(int initialCapacity) {
        if (initialCapacity <= 0) {
            throw new IllegalArgumentException("initialCapacity must be positive");
        }
        this.values = new long[initialCapacity];
    }

    public void add(long value) {
        ensureCapacity(size + 1);
        values[size++] = value;
    }

    public void addAll(LongArray other) {
        if (other == null || other.size == 0) {
            return;
        }
        ensureCapacity(size + other.size);
        System.arraycopy(other.values, 0, values, size, other.size);
        size += other.size;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public long[] toArray() {
        return Arrays.copyOf(values, size);
    }

    private void ensureCapacity(int capacity) {
        if (capacity <= values.length) {
            return;
        }
        int newCapacity = values.length;
        while (newCapacity < capacity) {
            newCapacity = newCapacity * 2;
        }
        values = Arrays.copyOf(values, newCapacity);
    }
}
