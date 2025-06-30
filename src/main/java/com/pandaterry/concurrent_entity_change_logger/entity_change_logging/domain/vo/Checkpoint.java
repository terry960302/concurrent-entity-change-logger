package com.pandaterry.concurrent_entity_change_logger.entity_change_logging.domain.vo;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class Checkpoint {
    private  long position;
    private  LocalDateTime timestamp;

    public static Checkpoint of(long position, LocalDateTime timestamp) {
        return new Checkpoint(position, timestamp);
    }

    public static Checkpoint current(long position) {
        return new Checkpoint(position, LocalDateTime.now());
    }

    public boolean isValid() {
        return position >= 0 && timestamp != null;
    }

    public boolean isOlderThan(LocalDateTime dateTime) {
        return timestamp.isBefore(dateTime);
    }

}