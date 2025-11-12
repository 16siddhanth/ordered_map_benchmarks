# Project Overview and Execution Guide

This document explains how to build the codebase, run the benchmark harness, and interpret the artefacts produced by the "Comparison of Thread-Safe Ordered Map Designs" project.

## Prerequisites

- Java Development Kit (JDK) 17 or later (`java -version` should report 17.x).
- Apache Maven 3.8 or newer available on the command line (`mvn -v`).
- A POSIX-like shell (examples below use `zsh`/`bash`).

Clone or unzip the repository, then change into the project directory:

```bash
cd /path/to/ordered-map-benchmarks
```

## Build the Project

Compile production code and tests, then produce a shaded (self-contained) benchmark JAR:

```bash
mvn clean package
```

The shaded artefact lands at `target/ordered-map-benchmarks-1.0-SNAPSHOT.jar`.

## Run the Benchmark Harness

Execute the runner with command-line flags to choose map implementations, workloads, thread counts, and durations.

### Quick Example

```bash
java -jar target/ordered-map-benchmarks-1.0-SNAPSHOT.jar \
  --workloads read-heavy,mixed,range-heavy \
  --maps global,sharded,skiplist,tinystm,stm \
  --threads 1,4,8 \
  --duration 5s
```

### Using the Bundled Configuration

A reusable scenario lives at `src/main/resources/config/default-benchmarks.json`. Load it and override selected options:

```bash
java -jar target/ordered-map-benchmarks-1.0-SNAPSHOT.jar \
  --config src/main/resources/config/default-benchmarks.json \
  --threads 2,6,12 \
  --csv results/custom.csv \
  --json results/custom.json
```

### Output

- Console: human-readable summary table (throughput, latency quantiles, STM stats).
- CSV/JSON (optional): structured datasets suitable for plotting or further analysis. Paths are resolved relative to the working directory; parent folders are created automatically.

## Run Automated Tests

```bash
mvn test
```

Tests cover functional behaviour of each map implementation (CRUD, range queries, concurrency) and a smoke run of the benchmark harness.

## Repository Layout Highlights

- `src/main/java/org/example/orderedmap/api`: Shared interfaces and metrics types.
- `src/main/java/org/example/orderedmap/locking`: Lock-based map variants.
- `src/main/java/org/example/orderedmap/skiplist`: Wrapper over `ConcurrentSkipListMap`.
- `src/main/java/org/example/orderedmap/stm`: Tiny STM runtime and Multiverse-backed variant.
- `src/main/java/org/example/orderedmap/benchmarks`: Workload definitions and runner.
- `src/main/resources/config`: JSON benchmark scenarios.
- `src/test/java`: JUnit 5 tests.
- `doc/report.md`: Design narrative referenced by the README.

## Troubleshooting Tips

- **Missing `mvn`**: Install Maven (e.g., `brew install maven` on macOS) or use the Maven wrapper if added later.
- **JDK mismatch**: Ensure the active `JAVA_HOME` points to a JDK 17 installation.
- **Jackson dependency issues**: Re-run `mvn clean package` to download dependencies; ensure outbound internet access is available.

With Maven and Java properly configured, the project builds from scratch, executes the benchmark suite, and outputs results without additional setup.
