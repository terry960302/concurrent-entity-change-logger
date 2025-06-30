package com.pandaterry.concurrent_entity_change_logger.shared.error;

public interface ErrorCode {
    int getStatus();
    String getCode();
    String getMessage();

    default String getFullMessage() {
        return String.format("[%s] %s", getCode(), getMessage());
    }
}
