package org.example.orderedmap.benchmarks;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class BenchmarkRunnerTest {

    @Test
    void runAllProducesResults() {
        BenchmarkConfig config = BenchmarkConfig.builder()
                .withMapTypes(List.of(MapType.GLOBAL))
                .withWorkloads(List.of(WorkloadProfile.READ_HEAVY))
                .withThreadCounts(List.of(2))
                .withInitialSize(256)
                .withKeySpace(1024)
                .withRangeWidth(32)
                .withWarmup(Duration.ofMillis(100))
                .withRunDuration(Duration.ofMillis(200))
        .withRepeats(2)
                .withSeed(1234L)
                .build();

        BenchmarkRunner runner = new BenchmarkRunner();
        BenchmarkResult result = runner.runAll(config);
        assertNotNull(result);
        assertFalse(result.runs().isEmpty());
        assertEquals(2, result.runs().size());
        RunResult run = result.runs().get(0);
        assertTrue(run.totalOperations() > 0);
        assertTrue(run.operationsPerSecond() > 0.0d);
        assertEquals(1, run.repeat());
        assertEquals(2, result.runs().get(1).repeat());
    }
}
