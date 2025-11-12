package org.example.orderedmap.stm.tiny;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Minimal TL2-inspired STM runtime supporting read/write transactions on {@link TinyStmRef} entries.
 */
public final class TinyStm {

    private static final int YIELD_THRESHOLD = 32;

    private final AtomicLong globalClock = new AtomicLong();
    private final AtomicLong commitCount = new AtomicLong();
    private final AtomicLong abortCount = new AtomicLong();
    private final AtomicLong maxRetries = new AtomicLong();

    <T> T execute(TinyCallable<T> body) {
        int attempts = 0;
        while (true) {
            attempts++;
            Transaction tx = new Transaction(globalClock.get());
            try {
                T result = body.call(tx);
                if (tx.commit(globalClock)) {
                    commitCount.incrementAndGet();
                    updateMaxRetries(attempts - 1);
                    return result;
                }
            } catch (RetryException ignore) {
                // fall-through to retry
            }
            abortCount.incrementAndGet();
            if (attempts > YIELD_THRESHOLD) {
                java.util.concurrent.locks.LockSupport.parkNanos(1L);
            }
        }
    }

    public long getCommitCount() {
        return commitCount.get();
    }

    public long getAbortCount() {
        return abortCount.get();
    }

    public long getMaxRetries() {
        return maxRetries.get();
    }

    public void resetStats() {
        commitCount.set(0L);
        abortCount.set(0L);
        maxRetries.set(0L);
    }

    private void updateMaxRetries(int retries) {
        long current;
        do {
            current = maxRetries.get();
            if (retries <= current) {
                return;
            }
        } while (!maxRetries.compareAndSet(current, retries));
    }

    /**
     * Transactional context storing per-transaction read and write sets.
     */
    static final class Transaction {
        private final long startVersion;
    private final Map<TinyStmRef<?>, Long> readSet = new IdentityHashMap<>();
    private final Map<TinyStmRef<?>, Object> writeSet = new IdentityHashMap<>();

        private Transaction(long startVersion) {
            this.startVersion = startVersion;
        }

        @SuppressWarnings("unchecked")
        <T> T read(TinyStmRef<T> ref) {
            if (writeSet.containsKey(ref)) {
                return (T) writeSet.get(ref);
            }
            long version;
            T value;
            while (true) {
                version = ref.getVersion();
                value = ref.getValue();
                long verify = ref.getVersion();
                if (version == verify) {
                    break;
                }
            }
            if (version > startVersion) {
                throw new RetryException();
            }
            readSet.putIfAbsent(ref, version);
            return value;
        }

        <T> void write(TinyStmRef<T> ref, T value) {
            writeSet.put(ref, value);
        }

        private boolean commit(AtomicLong clock) {
            if (!validate()) {
                return false;
            }
            if (writeSet.isEmpty()) {
                return true;
            }
            List<TinyStmRef<?>> refs = new ArrayList<>(writeSet.keySet());
            refs.sort(Comparator.comparingInt(System::identityHashCode));
            List<TinyStmRef<?>> locked = new ArrayList<>(refs.size());
            for (TinyStmRef<?> ref : refs) {
                if (ref.tryLock()) {
                    locked.add(ref);
                } else {
                    unlockAll(locked);
                    return false;
                }
            }
            try {
                if (!validate()) {
                    return false;
                }
                long newVersion = clock.incrementAndGet();
                for (TinyStmRef<?> ref : refs) {
                    @SuppressWarnings("unchecked")
                    TinyStmRef<Object> typed = (TinyStmRef<Object>) ref;
                    Object value = writeSet.get(ref);
                    typed.setValue(value, newVersion);
                }
                return true;
            } finally {
                unlockAll(locked);
            }
        }

        private boolean validate() {
            for (Map.Entry<TinyStmRef<?>, Long> entry : readSet.entrySet()) {
                TinyStmRef<?> ref = entry.getKey();
                long expected = entry.getValue();
                if (writeSet.containsKey(ref)) {
                    continue;
                }
                if (ref.getVersion() != expected && ref.getVersion() > startVersion) {
                    return false;
                }
            }
            return true;
        }

        private void unlockAll(List<TinyStmRef<?>> locked) {
            for (int i = locked.size() - 1; i >= 0; i--) {
                locked.get(i).unlock();
            }
        }
    }

    @FunctionalInterface
    interface TinyCallable<T> extends Callable<T> {
        T call(Transaction tx);

        @Override
        default T call() throws Exception {
            throw new UnsupportedOperationException("Use call(Transaction)");
        }
    }

    private static final class RetryException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
