package org.example.orderedmap.benchmarks;

import java.util.Arrays;

/**
 * Captures latency aggregates calculated from microsecond samples.
 */
final class LatencyStats {

    private final double meanMicros;
    private final long p50Micros;
    private final long p95Micros;
    private final long p99Micros;

    private LatencyStats(double meanMicros, long p50Micros, long p95Micros, long p99Micros) {
        this.meanMicros = meanMicros;
        this.p50Micros = p50Micros;
        this.p95Micros = p95Micros;
        this.p99Micros = p99Micros;
    }

    static LatencyStats fromMicros(long[] samples) {
        if (samples == null || samples.length == 0) {
            return new LatencyStats(0.0d, 0L, 0L, 0L);
        }
        long[] copy = Arrays.copyOf(samples, samples.length);
        Arrays.sort(copy);
        long sum = 0L;
        for (long sample : copy) {
            sum += sample;
        }
        double mean = sum / (double) copy.length;
        long p50 = percentile(copy, 0.50d);
        long p95 = percentile(copy, 0.95d);
        long p99 = percentile(copy, 0.99d);
        return new LatencyStats(mean, p50, p95, p99);
    }

    double meanMicros() {
        return meanMicros;
    }

    long p50Micros() {
        return p50Micros;
    }

    long p95Micros() {
        return p95Micros;
    }

    long p99Micros() {
        return p99Micros;
    }

    private static long percentile(long[] sorted, double quantile) {
        if (sorted.length == 0) {
            return 0L;
        }
        double index = quantile * (sorted.length - 1);
        int lower = (int) Math.floor(index);
        int upper = (int) Math.ceil(index);
        if (lower == upper) {
            return sorted[lower];
        }
        double fraction = index - lower;
        return (long) Math.round(sorted[lower] + fraction * (sorted[upper] - sorted[lower]));
    }
}
