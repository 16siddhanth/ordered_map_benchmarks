package org.example.orderedmap.benchmarks;

import java.util.Arrays;
import java.util.Locale;
import java.util.SplittableRandom;

/**
 * Encapsulates the probability mix for a named workload.
 */
public enum WorkloadProfile {

    READ_HEAVY("read-heavy", 0.90, 0.05, 0.05, 0.0),
    WRITE_HEAVY("write-heavy", 0.20, 0.40, 0.40, 0.0),
    MIXED("mixed", 0.60, 0.20, 0.20, 0.0),
    RANGE_HEAVY("range-heavy", 0.30, 0.10, 0.10, 0.50);

    private final String id;
    private final double[] cumulative;

    WorkloadProfile(String id, double getWeight, double putWeight, double removeWeight, double rangeWeight) {
        this.id = id;
        double total = getWeight + putWeight + removeWeight + rangeWeight;
        if (Math.abs(total - 1.0d) > 1e-9) {
            throw new IllegalArgumentException("weights must sum to 1.0");
        }
        this.cumulative = new double[] {
                getWeight,
                getWeight + putWeight,
                getWeight + putWeight + removeWeight,
                1.0d
        };
    }

    public String id() {
        return id;
    }

    public OperationType chooseOperation(SplittableRandom random) {
        double draw = random.nextDouble();
        if (draw < cumulative[0]) {
            return OperationType.GET;
        }
        if (draw < cumulative[1]) {
            return OperationType.PUT;
        }
        if (draw < cumulative[2]) {
            return OperationType.REMOVE;
        }
        return OperationType.RANGE;
    }

    public static WorkloadProfile fromId(String id) {
        String normalized = id.toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(profile -> profile.id.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown workload profile: " + id));
    }
}
