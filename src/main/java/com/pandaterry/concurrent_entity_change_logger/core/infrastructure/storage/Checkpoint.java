package com.pandaterry.concurrent_entity_change_logger.core.infrastructure.storage;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class Checkpoint {
    private  long position;
    private  LocalDateTime timestamp;
}