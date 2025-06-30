package com.pandaterry.concurrent_entity_change_logger.entity_change_logging.domain.vo;

import lombok.Value;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Value
public class ContextData {
    private final Map<String, Object> data;

    private ContextData(Map<String, Object> data) {
        this.data = Map.copyOf(data);
    }

    public static ContextData empty() {
        return new ContextData(Map.of());
    }

    public static ContextData of(Map<String, Object> data) {
        Objects.requireNonNull(data, "Context data cannot be null");
        return new ContextData(data);
    }

    public static ContextData from(Map<String, Object> sourceMap) {
        Map<String, Object> filtered = new HashMap<>();
        sourceMap.entrySet().stream()
                .filter(entry -> !isSystemField(entry.getKey()))
                .forEach(entry -> filtered.put(entry.getKey(), entry.getValue()));
        return new ContextData(filtered);
    }

    public ContextData mergeWith(ContextData other) {
        if (other.isEmpty()) {
            return this;
        }

        Map<String, Object> merged = new HashMap<>(this.data);
        merged.putAll(other.data);
        return new ContextData(merged);
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }

    public Map<String, Object> getData() {
        return data;
    }

    private static boolean isSystemField(String key) {
        return "threadName".equals(key) || "hostname".equals(key) || "submittedAt".equals(key);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContextData that)) return false;
        return Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data);
    }
}
