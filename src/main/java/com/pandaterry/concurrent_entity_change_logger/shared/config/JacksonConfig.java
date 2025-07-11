package com.pandaterry.concurrent_entity_change_logger.shared.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
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

        // 알 수 없는 프로퍼티 무시 (하위 호환성)
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // null 값 무시
        mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);

        return mapper;
    }
}
