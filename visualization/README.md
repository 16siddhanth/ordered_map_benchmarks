# Benchmark Visualization Dashboard

This folder contains a web-based dashboard to run benchmarks and visualize the results in real-time.

## Prerequisites

- Python 3.8+
- Java 17+ (for running the benchmarks)
- Maven (to build the project)

## Setup

1. Create a virtual environment:
   ```bash
   python3 -m venv .venv
   source .venv/bin/activate
   ```

2. Install dependencies:
   ```bash
   pip install -r requirements.txt
   ```

## Running the Dashboard

1. Start the Flask server:
   ```bash
   python app.py
   ```

2. Open your browser and navigate to:
   http://localhost:5000

## Features

- **Run Benchmark**: Trigger the benchmark script directly from the browser.
- **Real-time Logs**: Watch the benchmark progress in a terminal-like interface.
- **Interactive Charts**: View Throughput and Latency graphs generated from the benchmark results.
