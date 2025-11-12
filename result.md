# Benchmark Results and Analysis

This document captures the compact benchmark run you executed, the numeric results, derived metrics (abort rates and speedups), observations, inferences, measurement caveats, and recommended follow-ups.

## Test configuration (compact run)
- Jar: `target/ordered-map-benchmarks-1.0-SNAPSHOT.jar` (shaded)
- Maps run: `global` (GlobalLockOrderedMap), `tinystm` (TinyStmOrderedMap)
- Workloads: `read-heavy`, `mixed`
- Threads: `1`, `4`
- Warmup: `0s`
- Measurement duration: `1s`
- Other defaults: `initialSize=10000`, `keySpace=65536`, `rangeWidth=256`

## Raw results (per-run)
(Each run printed: Map, Workload, Threads, Operations, Ops/sec, Duration(ms), Avg(us), P50(us), P95(us), P99(us), STM commits, STM aborts, Max retries)

| Map     | Workload   | Threads | Operations | Ops/sec    | Duration(ms) | Avg(us) | P50 | P95 | P99 | STM commits | STM aborts | Max retries |
|---------|------------|--------:|-----------:|-----------:|--------------:|--------:|----:|----:|----:|------------:|-----------:|------------:|
| global  | read-heavy | 1       | 6,796,351  | 6,793,973.11| 1000         | 0.00    | 0   | 0   | 0   | 0           | 0         | 0           |
| global  | read-heavy | 4       | 2,659,657  | 2,659,337.10| 1000         | 1.16    | 0   | 10  | 16  | 0           | 0         | 0           |
| global  | mixed      | 1       | 5,991,310  | 5,990,868.42| 1000         | 0.00    | 0   | 0   | 0   | 0           | 0         | 0           |
| global  | mixed      | 4       | 1,867,927  | 1,867,716.96| 1000         | 1.79    | 0   | 13  | 18  | 0           | 0         | 0           |
| tinystm | read-heavy | 1       | 3,368,945  | 3,368,617.96| 1000         | 0.02    | 0   | 0   | 1   | 3,368,945   | 0         | 0           |
| tinystm | read-heavy | 4       | 9,266,802  | 9,265,898.57| 1000         | 0.03    | 0   | 0   | 1   | 9,266,802   | 26        | 1           |
| tinystm | mixed      | 1       | 3,035,220  | 3,035,016.53| 1000         | 0.01    | 0   | 0   | 0   | 3,035,220   | 0         | 0           |
| tinystm | mixed      | 4       | 8,476,911  | 8,475,943.68| 1000         | 0.03    | 0   | 0   | 1   | 8,476,911   | 107       | 1           |

> Note: small differences between "Operations" and commit counts arise from reporting formatting and are negligible for analysis here.

## Derived metrics
### Abort rates (Tiny STM)
- read-heavy, 4 threads: abort rate = 26 / (9,266,802 + 26) ≈ 2.81e-6 ≈ 0.000281%
- mixed, 4 threads: abort rate = 107 / (8,476,911 + 107) ≈ 1.26e-5 ≈ 0.00126%

These abort rates are extremely low for these runs, indicating few transactional conflicts in the synthetic workloads and keyspace used.

### Scaling / speedup (1 -> 4 threads)
Throughput speedup = (ops at 4 threads) / (ops at 1 thread)

- global, read-heavy: 2.659M / 6.796M ≈ 0.39× (throughput dropped under 4 threads due to contention)
- tinystm, read-heavy: 9.267M / 3.369M ≈ 2.75× (good parallel scaling)

- global, mixed: 1.868M / 5.991M ≈ 0.31×
- tinystm, mixed: 8.477M / 3.035M ≈ 2.79×

Interpretation: GlobalLockOrderedMap shows degraded aggregate throughput as threads increase (a contention bottleneck). TinyStmOrderedMap scales positively for these workloads and key distributions.

### Latency notes
- The per-op microsecond latencies reported (Avg, P50, P95, P99) are often 0.00 or small because many operations complete in sub-microsecond times; sampling and clock resolution cause rounding to 0.00 µs.
- Use longer runs and higher-resolution timing if you need precise latency numbers; treat the printed latencies as indicative rather than exact for sub-microsecond operations.

## Observations & inferences
1. Single-threaded throughput:
   - `global` performs best on a single thread (lowest overhead).
   - `tinystm` has extra per-transaction overhead (read/write sets, validation) so single-thread absolute throughput is lower.

2. Multi-threaded behavior:
   - `global` performance drops when increasing threads from 1→4. This indicates contention on the single global lock causes serialization.
   - `tinystm` shows good scaling (≈2.7–2.8×) for 1→4 threads in both read-heavy and mixed workloads. For these workloads and the chosen key space the optimistic STM design avoids a global bottleneck and parallelizes well.

3. Conflict & abort behavior:
   - Measured abort rates for Tiny STM are negligible (<<0.01%) in these compact runs, implying a large key space and randomized accesses produce few conflicts.
   - Mixed workload produced slightly more aborts than pure read-heavy, as expected.

4. Trade-offs indicated by results:
   - Coarse-grained locking (GlobalLock) is simple and efficient for lower parallelism but becomes a scalability bottleneck under contention.
   - Optimistic STM has overhead per transaction but can significantly outperform a coarse lock under low-conflict parallel workloads and when transactions are small.

## Caveats and measurement limits
- Very short measurement window (1s) and zero warmup (0s) make results sensitive to JVM startup and JIT effects.
- Clock resolution and sample rounding hide sub-microsecond latencies; throughput (ops/sec) is the more stable metric here.
- No CPU thread pinning—OS scheduling noise can alter per-run variance.
- Synthetic integer-only workload: real workloads (larger objects, serialization, I/O) will change absolute numbers and conflict patterns.
- The `tinystm` implementation uses optimistic validation and per-entry locks at commit; larger transactions (e.g., wide range queries) will increase aborts and reduce throughput.

## Recommended short follow-ups (fast but informative)
Run these to get more stable and varied data (still small & fast):

1) Longer measurement with warmup (same compact map set):
```bash
java -jar target/ordered-map-benchmarks-1.0-SNAPSHOT.jar \
  --maps global,tinystm \
  --workloads read-heavy,mixed \
  --threads 1,4 \
  --warmup 2s --duration 5s \
  --csv results/quick_warm.csv
```

2) Stress writes to force conflicts (short):
```bash
java -jar target/ordered-map-benchmarks-1.0-SNAPSHOT.jar \
  --maps tinystm,global \
  --workloads write-heavy \
  --threads 1,2,4 \
  --warmup 1s --duration 5s
```

3) Range-heavy behavior (transactions touch many refs):
```bash
java -jar target/ordered-map-benchmarks-1.0-SNAPSHOT.jar \
  --maps tinystm,sharded,skiplist \
  --workloads range-heavy \
  --threads 1,4 \
  --range-width 1024 \
  --warmup 1s --duration 5s
```

4) Repeat runs (stability): run each configuration 3 times and compute mean & stddev.

## Practical recommendation (based on compact runs)
- If your real workload is low-concurrency or you prioritize minimal overhead, prefer a simple global-locked `TreeMap` wrapper (GlobalLockOrderedMap).
- If your workload is read-heavy, parallel, and conflicts are rare, the optimistic Tiny STM may provide much better throughput at scale while preserving composability (transactions).
- For write-heavy or wide-range transactional workloads expect STM aborts; evaluate sharded/skip-list approaches for those scenarios.

---

If you want I can:
- Re-run the recommended short follow-ups now and add their outputs to this file.
- Add a small script (`run_quick.sh`) to automate repeated runs and aggregate CSVs into a summary.
- Implement a `--repeats N` harness flag to automatically run each config N times and produce mean/stddev.

Which follow-up should I do next?