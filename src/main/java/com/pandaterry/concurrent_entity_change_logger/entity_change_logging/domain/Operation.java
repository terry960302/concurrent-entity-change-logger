package com.pandaterry.concurrent_entity_change_logger.entity_change_logging.domain;

import java.util.Set;

public enum Operation {
    CREATE("생성", Priority.NORMAL, Set.of("INSERT", "SAVE")),
    UPDATE("수정", Priority.LOW, Set.of("UPDATE", "MERGE")),
    DELETE("삭제", Priority.HIGH, Set.of("DELETE", "REMOVE"));

    private final String description;
    private final Priority priority;
    private final Set<String> aliases;

    Operation(String description, Priority priority, Set<String> aliases) {
        this.description = description;
        this.priority = priority;
        this.aliases = aliases;
    }

    public boolean requiresOldEntity() {
        return this == UPDATE || this == DELETE;
    }

    public boolean requiresNewEntity() {
        return this == CREATE || this == UPDATE;
    }

    public boolean isHighPriority() {
        return priority == Priority.HIGH;
    }

    public boolean matchesAlias(String alias) {
        return aliases.contains(alias.toUpperCase());
    }

    public static Operation fromAlias(String alias) {
        for (Operation op : values()) {
            if (op.matchesAlias(alias)) {
                return op;
            }
        }
        throw new IllegalArgumentException("Unknown operation alias: " + alias);
    }

    public String getDescription() {
        return description;
    }

    public Priority getPriority() {
        return priority;
    }

    public Set<String> getAliases() {
        return aliases;
    }

    public enum Priority {
        LOW(1),
        NORMAL(2),
        HIGH(3);

        private final int level;

        Priority(int level) {
            this.level = level;
        }

        public int getLevel() {
            return level;
        }

        public boolean isHigherThan(Priority other) {
            return this.level > other.level;
        }
    }
}
