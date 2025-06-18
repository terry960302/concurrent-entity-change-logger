package com.pandaterry.concurrent_entity_change_logger.core.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // LocalDateTime 등 Java 8 날짜 타입 지원
        mapper.registerModule(new JavaTimeModule());

        // timestamp 대신 ISO-8601 문자열 사용 (ex: 2025-06-18T15:30:00)
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);


        return mapper;
    }
}
