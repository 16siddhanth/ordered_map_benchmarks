#!/usr/bin/env python3
"""Generate throughput and latency graphs from benchmark CSV output."""

from __future__ import annotations

import argparse
from pathlib import Path
from typing import Iterable

import matplotlib.pyplot as plt
import pandas as pd


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Render graphs from ordered-map benchmark CSV output",
        formatter_class=argparse.ArgumentDefaultsHelpFormatter,
    )
    parser.add_argument(
        "--input",
        "-i",
        type=Path,
        default=Path("results/full_matrix.csv"),
        help="Path to the CSV file emitted by BenchmarkRunner",
    )
    parser.add_argument(
        "--output",
        "-o",
        type=Path,
        default=Path("results/graphs"),
        help="Directory where graphs will be written",
    )
    parser.add_argument(
        "--dpi",
        type=int,
        default=180,
        help="Resolution for generated PNG files",
    )
    return parser.parse_args()


def ensure_dataframe(csv_path: Path) -> pd.DataFrame:
    if not csv_path.exists():
        raise SystemExit(f"CSV file not found: {csv_path}")
    df = pd.read_csv(csv_path)
    required_columns = {
        "map",
        "workload",
        "threads",
        "repeat",
        "operations",
        "ops_per_sec",
        "duration_ms",
        "p95_us",
    }
    missing = required_columns.difference({col.lower(): col for col in df.columns}.keys())
    # Column names are already lower snake case when emitted by BenchmarkRunner;
    # this guard is here for future changes.
    if missing:
        raise SystemExit(f"CSV file {csv_path} is missing columns: {', '.join(sorted(missing))}")
    return df


def plots_for_workloads(df: pd.DataFrame, output: Path, dpi: int) -> Iterable[Path]:
    generated: list[Path] = []
    for workload in sorted(df['workload'].unique()):
        slice_df = df[df['workload'] == workload]
        summary = (
            slice_df.groupby(['map', 'threads'], as_index=False)
            ['ops_per_sec']
            .mean()
            .sort_values(['map', 'threads'])
        )
        fig, ax = plt.subplots(figsize=(8, 4.5))
        for map_name, group in summary.groupby('map'):
            group = group.sort_values('threads')
            ax.plot(group['threads'], group['ops_per_sec'], marker='o', label=map_name)
        ax.set_title(f"Throughput vs threads ({workload})")
        ax.set_xlabel("Threads")
        ax.set_ylabel("Ops / sec (avg across repeats)")
        ax.grid(True, linestyle='--', linewidth=0.5, alpha=0.6)
        ax.legend(title="Map")
        fig.tight_layout()
        target = output / f"ops_per_sec_{workload}.png"
        fig.savefig(target, dpi=dpi)
        plt.close(fig)
        generated.append(target)
    return generated


def aggregated_latency_plot(df: pd.DataFrame, output: Path, dpi: int) -> Path:
    summary = (
        df.groupby('map', as_index=False)
        ['p95_us']
        .mean()
        .sort_values('p95_us', ascending=True)
    )
    fig, ax = plt.subplots(figsize=(7, 4))
    ax.bar(summary['map'], summary['p95_us'], color='#3182CE')
    ax.set_title("Average p95 latency by map")
    ax.set_ylabel("microseconds")
    ax.set_xlabel("Map implementation")
    ax.grid(axis='y', linestyle='--', linewidth=0.5, alpha=0.5)
    fig.tight_layout()
    target = output / "latency_p95_by_map.png"
    fig.savefig(target, dpi=dpi)
    plt.close(fig)
    return target


def main() -> None:
    args = parse_args()
    df = ensure_dataframe(args.input)
    args.output.mkdir(parents=True, exist_ok=True)

    outputs = []
    outputs.extend(plots_for_workloads(df, args.output, args.dpi))
    outputs.append(aggregated_latency_plot(df, args.output, args.dpi))

    for path in outputs:
        print(f"wrote {path}")


if __name__ == "__main__":
    main()
