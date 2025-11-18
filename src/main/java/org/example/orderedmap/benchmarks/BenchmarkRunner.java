package org.example.orderedmap.benchmarks;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.example.orderedmap.api.MapMetrics;
import org.example.orderedmap.api.OrderedMap;
import org.example.orderedmap.util.LongArray;

/**
 * Entry point for the ordered map benchmark harness.
 */
public final class BenchmarkRunner {

    public static void main(String[] args) {
        try {
            BenchmarkConfig config = BenchmarkConfig.fromArgs(args);
            BenchmarkRunner runner = new BenchmarkRunner();
            BenchmarkResult result = runner.runAll(config);
            result.printSummary(System.out);
            if (config.csvOutput() != null) {
                result.writeCsv(config.csvOutput());
            }
            if (config.jsonOutput() != null) {
                result.writeJson(config.jsonOutput());
            }
        } catch (BenchmarkConfig.HelpException help) {
            BenchmarkConfig.printUsage(System.out);
        } catch (IllegalArgumentException | IOException ex) {
            System.err.println("Error: " + ex.getMessage());
            BenchmarkConfig.printUsage(System.err);
            System.exit(1);
        }
    }

    public BenchmarkResult runAll(BenchmarkConfig config) {
        List<RunResult> runs = new ArrayList<>();
        for (MapType mapType : config.mapTypes()) {
            for (WorkloadProfile workload : config.workloads()) {
                for (int threads : config.threadCounts()) {
                    for (int repeat = 1; repeat <= config.repeats(); repeat++) {
                        runs.add(runSingle(config, mapType, workload, threads, repeat));
                    }
                }
            }
        }
        return new BenchmarkResult(config, runs);
    }

    private RunResult runSingle(BenchmarkConfig config,
                                MapType mapType,
                                WorkloadProfile workload,
                                int threadCount,
                                int repeatIndex) {
        try (OrderedMap<Integer, Integer> map = mapType.create()) {
            seedData(map, config.initialSize());
            if (!config.warmupDuration().isZero()) {
                executePhase(map, config, workload, threadCount, config.warmupDuration(), false);
            }
            MapMetrics baseline = map.snapshotMetrics();
            ExecutionResult measurement = executePhase(map, config, workload, threadCount, config.runDuration(), true);
            MapMetrics metrics = map.snapshotMetrics().diff(baseline);
            double opsPerSecond = measurement.totalOperations / (measurement.durationNanos / 1_000_000_000.0d);
            LatencyStats latency = LatencyStats.fromMicros(measurement.latencies);
            long durationMillis = TimeUnit.NANOSECONDS.toMillis(measurement.durationNanos);
            return new RunResult(mapType, workload, threadCount, repeatIndex,
                    measurement.totalOperations, opsPerSecond, durationMillis, latency, metrics);
        }
    }

    private void seedData(OrderedMap<Integer, Integer> map, int initialSize) {
        for (int i = 0; i < initialSize; i++) {
            map.put(i, i);
        }
    }

    private ExecutionResult executePhase(OrderedMap<Integer, Integer> map,
                                         BenchmarkConfig config,
                                         WorkloadProfile workload,
                                         int threadCount,
                                         Duration duration,
                                         boolean collectSamples) {
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<WorkerResult>> futures = new ArrayList<>(threadCount);
        for (int workerId = 0; workerId < threadCount; workerId++) {
            final int id = workerId;
            futures.add(pool.submit(workerTask(map, config, workload, duration, collectSamples, ready, start, id)));
        }
        try {
            ready.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            pool.shutdownNow();
            throw new IllegalStateException("Interrupted while waiting for workers", e);
        }
        long phaseStart = System.nanoTime();
        start.countDown();
        pool.shutdown();
        try {
            boolean terminated = pool.awaitTermination(duration.toMillis() + 10_000L, TimeUnit.MILLISECONDS);
            if (!terminated) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            pool.shutdownNow();
        }
        long phaseEnd = System.nanoTime();
        long totalOperations = 0L;
    LongArray latencies = collectSamples ? new LongArray(threadCount * 512) : null;
        for (Future<WorkerResult> future : futures) {
            try {
                WorkerResult worker = future.get();
                totalOperations += worker.operations();
                if (collectSamples && latencies != null && worker.latencies() != null) {
                    latencies.addAll(worker.latencies());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                pool.shutdownNow();
                throw new IllegalStateException("Interrupted while gathering results", e);
            } catch (ExecutionException e) {
                pool.shutdownNow();
                throw new IllegalStateException("Worker execution failed", e.getCause());
            }
        }
        long[] samples = collectSamples && latencies != null ? latencies.toArray() : new long[0];
        return new ExecutionResult(totalOperations, phaseEnd - phaseStart, samples);
    }

    private Callable<WorkerResult> workerTask(OrderedMap<Integer, Integer> map,
                                              BenchmarkConfig config,
                                              WorkloadProfile workload,
                                              Duration duration,
                                              boolean collectSamples,
                                              CountDownLatch ready,
                                              CountDownLatch start,
                                              int workerId) {
        return () -> {
            SplittableRandom random = new SplittableRandom(config.seed() + workerId);
            LongArray samples = collectSamples ? new LongArray(1024) : null;
            ready.countDown();
            try {
                start.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Worker interrupted before start", e);
            }
            long deadline = System.nanoTime() + duration.toNanos();
            long operations = 0L;
            while (System.nanoTime() < deadline) {
                long opStart = collectSamples ? System.nanoTime() : 0L;
                performOperation(map, workload, random, config);
                if (collectSamples && samples != null) {
                    long micros = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - opStart);
                    samples.add(micros);
                }
                operations++;
            }
            return new WorkerResult(operations, samples);
        };
    }

    private void performOperation(OrderedMap<Integer, Integer> map,
                                  WorkloadProfile workload,
                                  SplittableRandom random,
                                  BenchmarkConfig config) {
        OperationType operation = workload.chooseOperation(random);
        int keySpace = config.keySpace();
        int key = keySpace == 0 ? 0 : random.nextInt(keySpace);
        switch (operation) {
            case GET -> map.get(key);
            case PUT -> map.put(key, random.nextInt());
            case REMOVE -> map.remove(key);
            case RANGE -> {
                int width = Math.max(1, config.rangeWidth());
                long candidate = (long) key + width;
                int upper = (int) Math.min((long) keySpace - 1L, candidate);
                if (upper < key) {
                    upper = key;
                }
                var result = map.rangeQuery(key, true, upper, true);
                if (result != null) {
                    result.size();
                }
            }
        }
    }

    private record ExecutionResult(long totalOperations, long durationNanos, long[] latencies) {
    }

    private record WorkerResult(long operations, LongArray latencies) {
    }
}
