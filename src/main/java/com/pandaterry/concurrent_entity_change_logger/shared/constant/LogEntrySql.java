package com.pandaterry.concurrent_entity_change_logger.shared.constant;

public class LogEntrySql {
    public static final String INSERT = """
            INSERT INTO log_entries (id, entity_name, entity_id, changes, operation, recorded_at, context)\s
                            VALUES (?::uuid, ?, ?, ?::jsonb, ?, ?, ?::jsonb)
                        """;

    private LogEntrySql() {
    }
}
