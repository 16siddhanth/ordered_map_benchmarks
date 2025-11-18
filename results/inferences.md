## 1. Read‑heavy throughput vs threads

(First plot: “Throughput vs threads (read‑heavy)”)

**Scaling behavior**

- **GlobalLockOrderedMap (global)**
  - Throughput **drops** as threads increase (line slopes downward).
  - Interpretation: one coarse lock becomes a **bottleneck**; more threads just add contention and context switches.

- **ShardedOrderedMap (sharded)**
  - Throughput **increases roughly linearly** from 1→4 threads.
  - Sharding successfully reduces contention; threads often hit different shards.

- **SkipListOrderedMap (skiplist)**
  - Starts a bit lower at 1 thread but becomes the **fastest** at 4 threads.
  - Lock‑free navigation + fine‑grained synchronization scales very well under reads.

- **TinyStmOrderedMap (tinystm)**
  - Scales nicely with threads; curve is close to sharded/skiplist.
  - STM overhead hurts a bit at 1 thread, but under concurrency it benefits from optimistic reads and low conflicts.

- **LibraryStmOrderedMap (stm)**
  - Similar trend to Tiny STM, slightly higher throughput at 4 threads.
  - A mature STM engine (Multiverse/Gamma) handles read‑heavy parallelism well when conflicts are rare.

**Takeaway for read‑dominated workloads**

- For **purely read‑heavy** workloads on multi‑core:
  - **SkipList**, **sharded**, and both **STM** variants are good choices.
  - **Global lock** is only reasonable for single‑thread or very light contention; it does not scale.

---

## 2. Mixed workload throughput vs threads

(Second plot: “Throughput vs threads (mixed)” – includes writes)

**Impact of writes**

- **GlobalLockOrderedMap**
  - Again throughput falls as threads increase.
  - Writes plus reads under a single lock lead to even more contention and serialization.

- **ShardedOrderedMap**
  - Still scales up, but **less steeply** than in read‑heavy.
  - Writes introduce more contention within each shard; sharding helps, but shared shards still get hot.

- **SkipListOrderedMap**
  - Throughput grows strongly and is **near the top** at 4 threads.
  - Skip list’s fine‑grained structure handles concurrent updates relatively well.

- **TinyStmOrderedMap**
  - Scales smoothly but stays below skiplist/stm at 4 threads.
  - Writes cause some conflicts; STM pays extra for validation and possible retries.

- **LibraryStmOrderedMap**
  - Tracks skiplist very closely and is also near the top at 4 threads.
  - In this mixed workload, conflicts are still low enough that STM retries don’t dominate.

**Takeaway for mixed workloads**

- Under **read+write** mixes:
  - **SkipList** and **library STM** look like the best performers in your quick run.
  - **Tiny STM** is competitive but a bit slower, likely due to simpler implementation/overheads.
  - **Sharded** is decent, especially at low/moderate threads.
  - **Global** remains the worst choice once you have multiple threads.

---

## 3. Average p95 latency by map

(Third plot: “Average p95 latency by map”)

- **GlobalLockOrderedMap**
  - Has by far the **highest p95 latency** (worst tail). Long bars here show that 5% of operations see noticeable delay under contention.
  - This matches the throughput story: a single lock causes queueing; some ops wait a long time.

- **ShardedOrderedMap**
  - Much **lower p95** than global (short bar), but still non‑zero.
  - Sharding reduces queueing, but hot shards can still have occasional long waits.

- **SkipListOrderedMap, TinyStmOrderedMap, LibraryStmOrderedMap**
  - Bars are essentially at **0–very low** microseconds in this summary.
  - Their 95th‑percentile latencies are so small that, at the scale and duration of this benchmark, they are close to measurement noise.
  - Interpretation: these three provide very **stable latency**—few slow operations—under the tested workloads.

**Tail‑latency takeaway**

- If you care about **latency SLOs**, not just throughput:
  - Avoid **GlobalLock** under concurrency; its tail latency is bad.
  - **Sharded** is a lot better but still has some spikes.
  - **SkipList** and both **STMs** produce extremely low p95 in your tests; they are much more predictable.

---

## Overall conclusions from these graphs

1. **Coarse vs fine‑grained vs STM**
   - Coarse global locking is simple but does not scale and has poor tail latency.
   - Sharding is a good incremental improvement: better throughput and latency but still lock‑based.
   - Skip lists and STM implementations are the best under parallel workloads, both in throughput and latency.

2. **STM viability**
   - Both Tiny STM and library STM **scale** under read‑heavy and mixed workloads.
   - Their p95 latency is excellent, and throughput becomes competitive or better than lock‑based designs once you have multiple threads.
   - This supports the claim that STM is a viable alternative for ordered maps in **low‑conflict** workloads.

3. **SkipList vs STM**
   - SkipList tends to edge out STM in raw throughput, especially as threads increase.
   - STM brings composability (transactions, range ops) and still performs very well, though with some overhead.

4. **Design guidance**
   - **Single-thread or low contention:** Global or simple lock may be OK.
   - **Moderate concurrency, mostly point operations:** Sharded map or SkipList.
   - **High concurrency, need composable transactions / range ops:** STM‑backed maps look attractive, especially the library STM.
