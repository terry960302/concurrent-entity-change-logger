package com.pandaterry.concurrent_entity_change_logger.core.domain;

public enum Operation {
    CREATE("생성"),
    UPDATE("수정"),
    DELETE("삭제");

    private final String description;

    Operation(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}