package org.example.orderedmap.benchmarks;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Represents the configuration for a benchmark session.
 */
public final class BenchmarkConfig {

    private final List<MapType> mapTypes;
    private final List<WorkloadProfile> workloads;
    private final List<Integer> threadCounts;
    private final int initialSize;
    private final int keySpace;
    private final int rangeWidth;
    private final Duration warmupDuration;
    private final Duration runDuration;
    private final int repeats;
    private final long seed;
    private final Path csvOutput;
    private final Path jsonOutput;

    private BenchmarkConfig(Builder builder) {
        this.mapTypes = List.copyOf(builder.mapTypes);
        this.workloads = List.copyOf(builder.workloads);
        this.threadCounts = List.copyOf(builder.threadCounts);
        this.initialSize = builder.initialSize;
        this.keySpace = builder.keySpace;
        this.rangeWidth = builder.rangeWidth;
        this.warmupDuration = builder.warmupDuration;
    this.runDuration = builder.runDuration;
    this.repeats = builder.repeats;
        this.seed = builder.seed;
        this.csvOutput = builder.csvOutput;
        this.jsonOutput = builder.jsonOutput;
    }

    public List<MapType> mapTypes() {
        return mapTypes;
    }

    public List<WorkloadProfile> workloads() {
        return workloads;
    }

    public List<Integer> threadCounts() {
        return threadCounts;
    }

    public int initialSize() {
        return initialSize;
    }

    public int keySpace() {
        return keySpace;
    }

    public int rangeWidth() {
        return rangeWidth;
    }

    public Duration warmupDuration() {
        return warmupDuration;
    }

    public Duration runDuration() {
        return runDuration;
    }

    public int repeats() {
        return repeats;
    }

    public long seed() {
        return seed;
    }

    public Path csvOutput() {
        return csvOutput;
    }

    public Path jsonOutput() {
        return jsonOutput;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static BenchmarkConfig fromArgs(String[] args) {
        Builder builder = builder();
        Path configPath = null;
        Map<String, String> overrides = new java.util.LinkedHashMap<>();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--help".equals(arg) || "-h".equals(arg)) {
                throw new HelpException();
            }
            if (!arg.startsWith("--")) {
                throw new IllegalArgumentException("Unrecognized argument: " + arg);
            }
            String key = arg.substring(2);
            if ("config".equals(key)) {
                String value = requireValue(args, ++i, key);
                configPath = Path.of(value);
                continue;
            }
            String value = requireValue(args, ++i, key);
            overrides.put(key, value);
        }
        if (configPath != null) {
            builder.applyConfigFile(configPath);
        }
        overrides.forEach(builder::applyOption);
        return builder.build();
    }

    static void printUsage(PrintStream out) {
        out.println("Usage: java -jar ordered-map-benchmarks.jar [options]\n");
        out.println("Options:");
        out.println("  --config <path>        Load configuration from JSON file");
        out.println("  --maps <a,b,c>        Comma-separated list of map types (global, sharded, skiplist, tinystm, stm)");
        out.println("  --workloads <...>     Comma-separated list of workloads (read-heavy, write-heavy, mixed, range-heavy)");
        out.println("  --threads <...>       Comma-separated thread counts (e.g. 1,4,8)");
        out.println("  --duration <value>    Measurement duration (e.g. 5s, 2m)");
        out.println("  --warmup <value>      Warmup duration (e.g. 2s)");
        out.println("  --initial-size <n>    Initial number of entries preloaded into each map");
        out.println("  --key-space <n>       Range of keys randomly chosen during workloads");
        out.println("  --range-width <n>     Width of generated range queries");
        out.println("  --seed <n>            Random seed for reproducible workloads");
        out.println("  --repeats <n>         Number of times to repeat each configuration (default 1)");
        out.println("  --csv <path>          Optional CSV output path");
        out.println("  --json <path>         Optional JSON output path");
        out.println("  --help                Display this help message");
    }

    private static String requireValue(String[] args, int index, String key) {
        if (index >= args.length) {
            throw new IllegalArgumentException("Missing value for --" + key);
        }
        return args[index];
    }

    private static Duration parseDuration(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.endsWith("ms")) {
            long amount = Long.parseLong(normalized.substring(0, normalized.length() - 2));
            return Duration.ofMillis(amount);
        }
        if (normalized.endsWith("s")) {
            long amount = Long.parseLong(normalized.substring(0, normalized.length() - 1));
            return Duration.ofSeconds(amount);
        }
        if (normalized.endsWith("m")) {
            long amount = Long.parseLong(normalized.substring(0, normalized.length() - 1));
            return Duration.ofMinutes(amount);
        }
        throw new IllegalArgumentException("Unsupported duration format: " + value);
    }

    private static List<Integer> parseThreadCounts(String value) {
        return java.util.Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public static final class Builder {
        private List<MapType> mapTypes = new ArrayList<>(List.of(MapType.values()));
        private List<WorkloadProfile> workloads = new ArrayList<>(List.of(WorkloadProfile.values()));
        private List<Integer> threadCounts = new ArrayList<>(List.of(1, 4, 8));
        private int initialSize = 10000;
        private int keySpace = 65536;
        private int rangeWidth = 128;
        private Duration warmupDuration = Duration.ofSeconds(2);
        private Duration runDuration = Duration.ofSeconds(5);
        private int repeats = 1;
        private long seed = 1337L;
        private Path csvOutput;
        private Path jsonOutput;

        public Builder withMapTypes(List<MapType> mapTypes) {
            this.mapTypes = new ArrayList<>(Objects.requireNonNull(mapTypes));
            return this;
        }

        public Builder withWorkloads(List<WorkloadProfile> workloads) {
            this.workloads = new ArrayList<>(Objects.requireNonNull(workloads));
            return this;
        }

        public Builder withThreadCounts(List<Integer> counts) {
            this.threadCounts = new ArrayList<>(Objects.requireNonNull(counts));
            return this;
        }

        public Builder withInitialSize(int initialSize) {
            this.initialSize = initialSize;
            return this;
        }

        public Builder withKeySpace(int keySpace) {
            this.keySpace = keySpace;
            return this;
        }

        public Builder withRangeWidth(int rangeWidth) {
            this.rangeWidth = rangeWidth;
            return this;
        }

        public Builder withWarmup(Duration duration) {
            this.warmupDuration = Objects.requireNonNull(duration, "warmup");
            return this;
        }

        public Builder withRunDuration(Duration duration) {
            this.runDuration = Objects.requireNonNull(duration, "duration");
            return this;
        }

        public Builder withRepeats(int repeats) {
            if (repeats <= 0) {
                throw new IllegalArgumentException("repeats must be positive");
            }
            this.repeats = repeats;
            return this;
        }

        public Builder withSeed(long seed) {
            this.seed = seed;
            return this;
        }

        public Builder withCsvOutput(Path path) {
            this.csvOutput = path;
            return this;
        }

        public Builder withJsonOutput(Path path) {
            this.jsonOutput = path;
            return this;
        }

        void applyOption(String key, String value) {
            switch (key) {
                case "maps" -> withMapTypes(MapType.parseList(value));
                case "workloads" -> withWorkloads(parseWorkloads(value));
                case "threads" -> withThreadCounts(parseThreadCounts(value));
                case "duration" -> withRunDuration(parseDuration(value));
                case "warmup" -> withWarmup(parseDuration(value));
                case "initial-size" -> withInitialSize(Integer.parseInt(value));
                case "key-space" -> withKeySpace(Integer.parseInt(value));
                case "range-width" -> withRangeWidth(Integer.parseInt(value));
                case "seed" -> withSeed(Long.parseLong(value));
                case "csv" -> withCsvOutput(Path.of(value));
                case "json" -> withJsonOutput(Path.of(value));
                case "repeats" -> withRepeats(Integer.parseInt(value));
                default -> throw new IllegalArgumentException("Unknown option: --" + key);
            }
        }

    void applyConfigFile(Path path) {
            try {
                ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                ConfigFile file = mapper.readValue(path.toFile(), ConfigFile.class);
                if (file.maps != null) {
                    withMapTypes(file.maps.stream().map(MapType::fromId).collect(Collectors.toList()));
                }
                if (file.workloads != null) {
                    withWorkloads(file.workloads.stream().map(WorkloadProfile::fromId).collect(Collectors.toList()));
                }
                if (file.threads != null) {
                    withThreadCounts(new ArrayList<>(file.threads));
                }
                if (file.duration != null) {
                    withRunDuration(parseDuration(file.duration));
                }
                if (file.warmup != null) {
                    withWarmup(parseDuration(file.warmup));
                }
                if (file.initialSize != null) {
                    withInitialSize(file.initialSize);
                }
                if (file.keySpace != null) {
                    withKeySpace(file.keySpace);
                }
                if (file.rangeWidth != null) {
                    withRangeWidth(file.rangeWidth);
                }
                if (file.seed != null) {
                    withSeed(file.seed);
                }
                if (file.csv != null) {
                    withCsvOutput(Path.of(file.csv));
                }
                if (file.json != null) {
                    withJsonOutput(Path.of(file.json));
                }
                if (file.repeats != null) {
                    withRepeats(file.repeats);
                }
            } catch (IOException io) {
                throw new IllegalArgumentException("Failed to read config file " + path + ": " + io.getMessage(), io);
            }
        }

        BenchmarkConfig build() {
            if (mapTypes.isEmpty()) {
                throw new IllegalArgumentException("At least one map type must be specified");
            }
            if (workloads.isEmpty()) {
                throw new IllegalArgumentException("At least one workload must be specified");
            }
            if (threadCounts.isEmpty()) {
                throw new IllegalArgumentException("At least one thread count must be specified");
            }
            if (threadCounts.stream().anyMatch(count -> count <= 0)) {
                throw new IllegalArgumentException("thread counts must be positive");
            }
            if (keySpace <= 0) {
                throw new IllegalArgumentException("keySpace must be positive");
            }
            if (initialSize > keySpace) {
                throw new IllegalArgumentException("initialSize must not exceed key space");
            }
            if (rangeWidth <= 0) {
                throw new IllegalArgumentException("rangeWidth must be positive");
            }
            if (runDuration.isZero() || runDuration.isNegative()) {
                throw new IllegalArgumentException("duration must be positive");
            }
            if (warmupDuration.isNegative()) {
                throw new IllegalArgumentException("warmup must not be negative");
            }
            if (repeats <= 0) {
                throw new IllegalArgumentException("repeats must be positive");
            }
            return new BenchmarkConfig(this);
        }

        private List<WorkloadProfile> parseWorkloads(String value) {
        return java.util.Arrays.stream(value.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(WorkloadProfile::fromId)
                    .collect(Collectors.toCollection(ArrayList::new));
        }
    }

    private static final class ConfigFile {
        List<String> maps;
        List<String> workloads;
        List<Integer> threads;
        String duration;
        String warmup;
        Integer initialSize;
        Integer keySpace;
        Integer rangeWidth;
        Long seed;
        String csv;
        String json;
        Integer repeats;
    }

    static class HelpException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
