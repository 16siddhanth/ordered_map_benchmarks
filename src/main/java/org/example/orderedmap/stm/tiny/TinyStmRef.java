package org.example.orderedmap.stm.tiny;

import java.util.concurrent.locks.ReentrantLock;

final class TinyStmRef<T> {
    private final ReentrantLock lock = new ReentrantLock();
    private volatile long version;
    private volatile T value;

    TinyStmRef(T initialValue, long initialVersion) {
        this.value = initialValue;
        this.version = initialVersion;
    }

    long getVersion() {
        return version;
    }

    T getValue() {
        return value;
    }

    void setValue(T newValue, long newVersion) {
        this.value = newValue;
        this.version = newVersion;
    }

    boolean tryLock() {
        return lock.tryLock();
    }

    void unlock() {
        lock.unlock();
    }
}
