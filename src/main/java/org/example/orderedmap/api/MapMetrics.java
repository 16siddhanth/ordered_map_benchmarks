package org.example.orderedmap.api;

import java.util.Objects;

/**
 * Immutable snapshot of auxiliary metrics exposed by an {@link OrderedMap} implementation.
 */
public final class MapMetrics {
    private final long stmCommits;
    private final long stmAborts;
    private final long maxRetries;

    private static final MapMetrics EMPTY = new MapMetrics(0L, 0L, 0L);

    public MapMetrics(long stmCommits, long stmAborts, long maxRetries) {
        this.stmCommits = stmCommits;
        this.stmAborts = stmAborts;
        this.maxRetries = maxRetries;
    }

    public long stmCommits() {
        return stmCommits;
    }

    public long stmAborts() {
        return stmAborts;
    }

    public long maxRetries() {
        return maxRetries;
    }

    public static MapMetrics empty() {
        return EMPTY;
    }

    public MapMetrics diff(MapMetrics baseline) {
        Objects.requireNonNull(baseline, "baseline");
        return new MapMetrics(
                stmCommits - baseline.stmCommits,
                stmAborts - baseline.stmAborts,
                Math.max(maxRetries, baseline.maxRetries)
        );
    }
}
