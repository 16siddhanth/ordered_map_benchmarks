# Comparison of Thread-Safe Ordered Map Designs — Technical Report

## 1. Introduction

Ordered maps are central to indexing services, caches, and OLTP engines. The advent of multicore processors demands concurrency control strategies that preserve ordering guarantees while exposing parallelism. This report accompanies the codebase implementing and benchmarking five ordered map variants: coarse-grained locking, sharded locking, lock-free skip list navigation, a custom Tiny STM, and a Multiverse-backed STM.

## 2. Problem Statement

We evaluate how different synchronization strategies impact throughput, latency, and abort behaviour across heterogeneous workloads:

- **Workloads:** read-heavy, write-heavy, mixed, and range-heavy
- **Operations:** single-key get/put/remove and contiguous range queries
- **Metrics:** operations per second, microsecond-level latency, STM commit/abort rates

## 3. Implementations

### 3.1 Ordered Map Interface

`OrderedMap<K, V>` defines the minimal API required by the benchmark harness: `get`, `put`, `remove`, `rangeQuery`, `size`, `clear`, and `close`. All implementations must deliver deterministic ordering semantics compatible with `NavigableMap`.

### 3.2 GlobalLockOrderedMap

- Backed by `TreeMap`
- Guarded by a single `ReentrantReadWriteLock`
- Baseline for correctness and minimal implementation effort
- Range queries simply snapshot the underlying map under a read lock

### 3.3 ShardedOrderedMap

- Partitioned into power-of-two shards by key hash
- Each shard holds a `TreeMap` protected by its own read/write lock
- Read/write operations lock only one shard; range queries lock all shards in shard-index order to maintain progress without deadlock
- Iterator merges per-shard ranges into a sorted result

### 3.4 SkipListOrderedMap

- Thin wrapper over `ConcurrentSkipListMap`
- Leverages lock-free navigation, offering strong progress under diverse contention profiles

### 3.5 TinyStmOrderedMap

- Embeds a minimalist TL2-inspired STM (`TinyStm`)
- Records per-transaction read and write sets, validates against a global version clock
- Per-entry `TinyStmRef` objects carry versioned state and fine-grained locks acquired only during commit
- Range queries traverse submaps within a transaction; aborts cause full retry
- Exposes runtime metrics (`commits`, `aborts`, `maxRetries`) for benchmark reporting

### 3.6 LibraryStmOrderedMap

- Wraps the Multiverse Gamma STM (v0.7.0)
- Uses `TxnRef` references for key/value pairs inside a `ConcurrentSkipListMap`
- Provides a reference implementation for production-grade STM

## 4. Benchmark Harness

`BenchmarkRunner` coordinates scenarios defined by `BenchmarkConfig` and `Workload`. Each benchmark run consists of:

1. Initial data seeding (configurable size and key/value distribution)
2. Ramp period to warm caches and the JIT compiler
3. Measurement window where worker threads execute operations based on workload probabilities
4. Result aggregation capturing throughput, latency, abort counts, and retry histograms

Workers rely on per-thread pseudo-random streams to eliminate cross-thread correlation. Range queries sample random half-open intervals whose span is derived from configuration (`rangeWidth`).

### Metrics Collected

| Metric | Description |
|--------|-------------|
| `opsPerSecond` | Aggregate throughput during measurement window |
| `p50`, `p95`, `p99` | Latency quantiles in microseconds |
| `stmCommits`, `stmAborts` | Tiny STM statistics (if map supports instrumentation) |
| `retriesPerTx` | Mean retries per committed Tiny STM transaction |

Outputs are printable, CSV-exportable, and optionally JSON-serialisable for integration with plotting tools.

## 5. Experimental Methodology (Suggested)

1. `mvn clean package`
2. Edit `config/default-benchmarks.json` (or pass CLI flags) to match the workload matrix
3. Run benchmark with multiple thread counts (1, 2, 4, 8, 16)
4. Capture CSV output and post-process with Python or R scripts for visualisation

When measuring latency, pin worker threads if possible and run on an otherwise idle machine. Repeat each configuration multiple times and report mean ± standard deviation.

## 6. Observations (Expected Trends)

- Coarse-grained locking degrades under high write contention due to exclusive lock ownership
- Sharding improves throughput until cross-shard range queries dominate
- Skip list maintains stable throughput, particularly for read-heavy workloads
- Tiny STM excels when write conflicts are rare; aborts increase with overlapping ranges
- Library STM serves as an upper baseline for transactional ergonomics at the cost of additional runtime overhead

## 7. Future Work

- Integrate off-the-shelf TL2 or JVSTM for comparison
- Implement adaptive sharding and dynamic range partitioning
- Extend benchmark to cover snapshot iterators and batched updates
- Collect hardware performance counters (LLC misses, branch mispredictions) for deeper analysis

## 8. References

- L. R. Shannon et al., "Skip Hash: A Fast Ordered Map Via Software Transactional Memory," arXiv:2410.07466v1 [cs.DC], 2024.
- N. Shavit and D. Touitou, "Software Transactional Memory," PODC 1995.
- M. Herlihy and N. Shavit, *The Art of Multiprocessor Programming*, 2nd Edition.
