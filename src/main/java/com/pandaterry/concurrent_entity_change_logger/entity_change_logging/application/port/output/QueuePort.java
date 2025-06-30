package com.pandaterry.concurrent_entity_change_logger.entity_change_logging.application.port.output;


import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.domain.model.LogEntry;

import java.util.List;

public interface QueuePort {
    boolean offer(LogEntry entry);
    int drainTo(List<LogEntry> batch, int maxElements);
    int remainingCapacity();
    int size();
    void init();
    void shutdown();
}
