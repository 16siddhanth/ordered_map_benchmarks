# Comparison of Thread-Safe Ordered Map Designs

This project implements and benchmarks multiple thread-safe ordered map designs with a focus on contrasting traditional locking, lock-free navigation, and Software Transactional Memory (STM) techniques. It draws inspiration from **Skip Hash: A Fast Ordered Map Via Software Transactional Memory** (arXiv:2410.07466v1, Oct 2024).

## Implementations

| Module | Synchronization Strategy | Notes |
|--------|---------------------------|-------|
| `GlobalLockOrderedMap` | Single read/write lock | Coarse-grained control suitable for light contention |
| `ShardedOrderedMap` | Lock striping across shards | Reduces contention; incurs overhead for range queries |
| `SkipListOrderedMap` | `ConcurrentSkipListMap` | Navigates without explicit locks |
| `TinyStmOrderedMap` | Custom TL2-style STM | Optimistic concurrency with per-entry versioning |
| `LibraryStmOrderedMap` | Multiverse Gamma STM | External STM with transactional references |

## Benchmarking

`BenchmarkRunner` executes configurable workloads:

- Read-heavy, write-heavy, mixed, and range-heavy mixes
- Varying thread counts
- Throughput (ops/sec) and latency (us/op)
- STM commit and abort rates (when available)

Use CLI flags to select workloads, runtime, map types, and report formats. Results can be exported as CSV and JSON for analysis.

## Quick Start

```bash
mvn clean package
java -jar target/ordered-map-benchmarks-1.0-SNAPSHOT.jar \
  --workloads read-heavy,mixed,range-heavy \
  --threads 1,4,8 \
  --duration 5s \
  --maps global,sharded,skiplist,tinystm,stm
```

Load the bundled scenario and override select knobs as needed:

```bash
java -jar target/ordered-map-benchmarks-1.0-SNAPSHOT.jar \
  --config src/main/resources/config/default-benchmarks.json \
  --threads 2,6,12 --csv results/custom.csv
```

For additional options:

```bash
java -jar target/ordered-map-benchmarks-1.0-SNAPSHOT.jar --help
```

## Project Layout

```
src/
  main/java/org/example/orderedmap/api              Interfaces and shared types
  main/java/org/example/orderedmap/locking          Lock-based map implementations
  main/java/org/example/orderedmap/skiplist         Skip-list wrapper
  main/java/org/example/orderedmap/stm/tiny         Tiny STM runtime and map
  main/java/org/example/orderedmap/stm/library      Multiverse-backed STM map
  main/java/org/example/orderedmap/benchmarks       Workloads and benchmark runner
  main/resources                                    Configuration defaults
  test/java/org/example/orderedmap                  Unit tests
```

## Documentation

See `doc/report.md` for design decisions, workload construction, measurement notes, and guidance on interpreting benchmark outputs.

## Requirements

- Java 17+
- Maven 3.8+

## Status

The baseline implementations and benchmarking harness are complete. Extend the workloads, add visualisations, or port the Tiny STM to other data structures to continue the exploration.
