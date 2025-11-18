#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")"/.. && pwd)"
JAR="$ROOT/target/ordered-map-benchmarks-1.0-SNAPSHOT.jar"
BENCH_DIR="$ROOT/results/benchmarks"
GRAPH_DIR="$ROOT/results/graphs"
CSV="$BENCH_DIR/all_maps_quick.csv"
JSON="$BENCH_DIR/all_maps_quick.json"

if [[ ! -f "$JAR" ]]; then
  echo "Benchmark jar not found at $JAR. Run 'mvn clean package' first." >&2
  exit 1
fi

mkdir -p "$BENCH_DIR" "$GRAPH_DIR"

JAVA_CMD=(
  java -jar "$JAR"
  --maps global,sharded,skiplist,tinystm,stm
  --workloads read-heavy,mixed
  --threads 1,4
  --warmup 1s
  --duration 3s
  --repeats 2
  --csv "$CSV"
  --json "$JSON"
)

printf '\n▶ Running benchmark suite (quick comparison)...\n'
"${JAVA_CMD[@]}"

PYTHON_BIN=${PYTHON_BIN:-"$ROOT/.venv/bin/python"}
if [[ ! -x "$PYTHON_BIN" ]]; then
  PYTHON_BIN="$(command -v python3 || true)"
fi

if [[ -z "$PYTHON_BIN" ]]; then
  echo "Python 3 interpreter not found; skipping graph generation." >&2
  exit 0
fi

printf '\n▶ Rendering graphs using %s...\n' "$PYTHON_BIN"
"$PYTHON_BIN" "$ROOT/scripts/plot_results.py" --input "$CSV" --output "$GRAPH_DIR"
