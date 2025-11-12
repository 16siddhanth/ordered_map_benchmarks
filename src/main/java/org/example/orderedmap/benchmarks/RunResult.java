package org.example.orderedmap.benchmarks;

import org.example.orderedmap.api.MapMetrics;

/**
 * Encapsulates the outcome of a single benchmark run.
 */
public record RunResult(
        MapType mapType,
        WorkloadProfile workload,
        int threadCount,
        long totalOperations,
        double operationsPerSecond,
        long durationMillis,
        LatencyStats latency,
        MapMetrics metrics) {
}
