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
  --repeats 2 \
  --maps global,sharded,skiplist,tinystm,stm
```

Load the bundled scenario and override select knobs as needed:

```bash
java -jar target/ordered-map-benchmarks-1.0-SNAPSHOT.jar \
  --config src/main/resources/config/default-benchmarks.json \
  --threads 2,6,12 \
  --repeats 3 \
  --csv results/custom.csv \
  --json results/custom.json
```

### Helpful flags

- `--repeats <n>` reruns every (map, workload, thread) tuple `n` times and annotates each row with its repeat index. Use this to gather variance statistics or to build charts from multiple samples.

## Results & Graphing

All five map designs (`global`, `sharded`, `skiplist`, `tinystm`, `stm`) run by default, and CSV/JSON exports automatically create the `results/` folders you point to. The "full matrix" command that sweeps 5 maps × 4 workloads × 4 thread counts × 3 repeats spends roughly 60 minutes on a laptop (240 runs × 15s), so feel free to trim duration, maps, or thread counts while iterating:

```bash
java -jar target/ordered-map-benchmarks-1.0-SNAPSHOT.jar \
  --maps global,sharded,skiplist,tinystm,stm \
  --workloads read-heavy,write-heavy,mixed,range-heavy \
  --threads 1,2,4,8 \
  --warmup 5s --duration 15s --repeats 3 \
  --csv results/full_matrix.csv \
  --json results/full_matrix.json
```

For faster smoke tests, drop to `--duration 3s --threads 1,2 --maps global,sharded --workloads read-heavy` and keep repeats at 1–2.

Once a CSV exists, install the lightweight Python toolchain and generate PNGs under `results/graphs/`:

```bash
python3 -m venv .venv
.venv/bin/pip install -r requirements.txt
.venv/bin/python scripts/plot_results.py --input results/full_matrix.csv --output results/graphs
```

The plotting script averages repeats per (map, workload, threads) tuple and renders throughput curves per workload plus an overall p95 latency bar chart.

### One-command fast comparison

Need every implementation side-by-side without waiting an hour? Build the jar, create the Python virtualenv once, and then run:

```bash
mvn clean package
python3 -m venv .venv
.venv/bin/pip install -r requirements.txt
./scripts/run_all_maps.sh
```

The script sweeps **all five maps** (`GlobalLockOrderedMap`, `ShardedOrderedMap`, `SkipListOrderedMap`, `TinyStmOrderedMap`, `LibraryStmOrderedMap`) across two representative workloads (`read-heavy`, `mixed`), two thread counts (`1`, `4`), and two repeats with short 3s measurements. It finishes in about 3 minutes on a laptop, writes CSV/JSON snapshots into `results/benchmarks/`, and immediately calls the plotting utility to refresh `results/graphs/*.png`. Adjust the script if you want longer durations or additional workloads.

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
