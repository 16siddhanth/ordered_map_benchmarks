package org.example.orderedmap.benchmarks;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.example.orderedmap.api.MapMetrics;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Aggregates the runs executed for a benchmark session.
 */
public final class BenchmarkResult {

    private final BenchmarkConfig config;
    private final List<RunResult> runs;

    BenchmarkResult(BenchmarkConfig config, List<RunResult> runs) {
        this.config = config;
        this.runs = List.copyOf(runs);
    }

    public List<RunResult> runs() {
        return Collections.unmodifiableList(runs);
    }

    public void printSummary(PrintStream out) {
    out.printf("%n%-10s %-12s %-7s %-12s %-12s %-12s %-9s %-9s %-9s %-12s %-12s %-10s %-11s%n",
        "Map", "Workload", "Threads", "Operations", "Ops/sec", "Duration(ms)",
        "Avg(us)", "P50(us)", "P95(us)", "P99(us)", "STM commits", "STM aborts", "Max retries");
        for (RunResult run : runs) {
            LatencyStats latency = run.latency();
            MapMetrics metrics = run.metrics();
        out.printf("%-10s %-12s %-7d %-12d %-12.2f %-12d %-9.2f %-9d %-9d %-12d %-12d %-10d %-11d%n",
                    run.mapType().id(),
                    run.workload().id(),
                    run.threadCount(),
                    run.totalOperations(),
                    run.operationsPerSecond(),
                    run.durationMillis(),
                    latency.meanMicros(),
                    latency.p50Micros(),
                    latency.p95Micros(),
                    latency.p99Micros(),
                    metrics.stmCommits(),
            metrics.stmAborts(),
            metrics.maxRetries());
        }
    }

    public void writeCsv(Path path) throws IOException {
        if (path == null) {
            return;
        }
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (var writer = Files.newBufferedWriter(path)) {
            writer.write("map,workload,threads,operations,ops_per_sec,duration_ms,avg_us,p50_us,p95_us,p99_us,stm_commits,stm_aborts,stm_max_retries");
            writer.newLine();
            for (RunResult run : runs) {
                LatencyStats latency = run.latency();
                MapMetrics metrics = run.metrics();
        writer.write(String.format(Locale.ROOT, "%s,%s,%d,%d,%.4f,%d,%.4f,%d,%d,%d,%d,%d,%d",
                        run.mapType().id(),
                        run.workload().id(),
                        run.threadCount(),
                        run.totalOperations(),
                        run.operationsPerSecond(),
                        run.durationMillis(),
                        latency.meanMicros(),
                        latency.p50Micros(),
                        latency.p95Micros(),
                        latency.p99Micros(),
                        metrics.stmCommits(),
                        metrics.stmAborts(),
                        metrics.maxRetries()));
                writer.newLine();
            }
        }
    }

    public void writeJson(Path path) throws IOException {
        if (path == null) {
            return;
        }
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        ObjectMapper mapper = new ObjectMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), new SerializableResult(config, runs));
    }

    private record SerializableResult(BenchmarkConfig config, List<RunResult> runs) {
    }
}
