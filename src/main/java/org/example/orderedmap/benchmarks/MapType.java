package org.example.orderedmap.benchmarks;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.example.orderedmap.api.OrderedMap;
import org.example.orderedmap.locking.GlobalLockOrderedMap;
import org.example.orderedmap.locking.ShardedOrderedMap;
import org.example.orderedmap.skiplist.SkipListOrderedMap;
import org.example.orderedmap.stm.library.LibraryStmOrderedMap;
import org.example.orderedmap.stm.tiny.TinyStmOrderedMap;

/**
 * Enumerates supported map implementations for the benchmark harness.
 */
public enum MapType {

    GLOBAL("global") {
        @Override
        public OrderedMap<Integer, Integer> create() {
            return new GlobalLockOrderedMap<>();
        }
    },
    SHARDED("sharded") {
        @Override
        public OrderedMap<Integer, Integer> create() {
            return new ShardedOrderedMap<>();
        }
    },
    SKIPLIST("skiplist") {
        @Override
        public OrderedMap<Integer, Integer> create() {
            return new SkipListOrderedMap<>();
        }
    },
    TINY_STM("tinystm") {
        @Override
        public OrderedMap<Integer, Integer> create() {
            return new TinyStmOrderedMap<>();
        }
    },
    LIBRARY_STM("stm") {
        @Override
        public OrderedMap<Integer, Integer> create() {
            return new LibraryStmOrderedMap<>();
        }
    };

    private final String id;

    MapType(String id) {
        this.id = id;
    }

    public abstract OrderedMap<Integer, Integer> create();

    public String id() {
        return id;
    }

    public static MapType fromId(String id) {
        String normalized = id.toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(type -> type.id.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown map type: " + id));
    }

    public static List<MapType> parseList(String csv) {
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .map(MapType::fromId)
                .collect(Collectors.toList());
    }
}
