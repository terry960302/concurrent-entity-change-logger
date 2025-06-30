package com.pandaterry.concurrent_entity_change_logger.entity_change_logging.adapter.out.persistence.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.domain.vo.ContextData;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Converter(autoApply = false)
@Component
@RequiredArgsConstructor
public class ContextJsonConverter implements AttributeConverter<ContextData, String> {

    private final ObjectMapper objectMapper;
    private static final TypeReference<Map<String, Object>> MAP_TYPE_REF =
            new TypeReference<Map<String, Object>>() {
            };

    private static final String EMPTY_CONTEXT_JSON = "{}";
    private static final int MAX_CONTEXT_SIZE = 10000; // 컨텍스트 데이터 크기 제한


    @Override
    public String convertToDatabaseColumn(ContextData contextData) {
        if (contextData == null || contextData.isEmpty()) {
            return EMPTY_CONTEXT_JSON;
        }

        try {
            Map<String, Object> data = contextData.getData();

            // 크기 제한 검사
            String json = objectMapper.writeValueAsString(data);
            if (json.length() > MAX_CONTEXT_SIZE) {
                log.warn("컨텍스트 데이터 크기({} 문자)가 제한치({})를 초과하여 축소합니다",
                        json.length(), MAX_CONTEXT_SIZE);
                return truncateContextData(data);
            }

            log.debug("컨텍스트 데이터를 JSON으로 변환했습니다: {} 필드 -> {} 문자",
                    data.size(), json.length());

            return json;

        } catch (JsonProcessingException e) {
            log.error("컨텍스트 데이터를 JSON으로 변환하는데 실패했습니다: {}", contextData.getData(), e);
            return createFallbackContextJson(contextData, e);
        }
    }

    @Override
    public ContextData convertToEntityAttribute(String dbValue) {
        if (isBlankOrEmpty(dbValue)) {
            return ContextData.empty();
        }

        try {
            Map<String, Object> rawMap = objectMapper.readValue(dbValue, MAP_TYPE_REF);

            // 보안상 위험한 필드 제거
            Map<String, Object> sanitizedMap = sanitizeContextData(rawMap);

            ContextData result = ContextData.of(sanitizedMap);

            log.debug("JSON을 컨텍스트 데이터로 변환했습니다: {} 문자 -> {} 필드",
                    dbValue.length(), result.getData().size());

            return result;

        } catch (Exception e) {
            log.error("JSON을 컨텍스트 데이터로 변환하는데 실패했습니다: {}", dbValue, e);
            return handleContextConversionFailure(dbValue, e);
        }
    }

    private String truncateContextData(Map<String, Object> data) {
        try {
            Map<String, Object> truncated = new HashMap<>();

            // 시스템 중요 필드 우선 보존
            String[] priorityFields = {"threadName", "hostname", "submittedAt", "userId", "sessionId"};

            for (String field : priorityFields) {
                if (data.containsKey(field)) {
                    truncated.put(field, data.get(field));
                }
            }

            // 남은 공간에 다른 필드들 추가
            int remainingSpace = MAX_CONTEXT_SIZE - objectMapper.writeValueAsString(truncated).length();

            for (Map.Entry<String, Object> entry : data.entrySet()) {
                if (!truncated.containsKey(entry.getKey())) {
                    String entryJson = objectMapper.writeValueAsString(Map.of(entry.getKey(), entry.getValue()));
                    if (entryJson.length() < remainingSpace) {
                        truncated.put(entry.getKey(), entry.getValue());
                        remainingSpace -= entryJson.length();
                    }
                }
            }

            // 축소됨을 표시
            truncated.put("_truncated", true);
            truncated.put("_original_field_count", data.size());

            log.info("컨텍스트 데이터를 {}개 필드에서 {}개 필드로 축소했습니다",
                    data.size(), truncated.size());

            return objectMapper.writeValueAsString(truncated);

        } catch (JsonProcessingException e) {
            log.error("컨텍스트 데이터 축소 중 오류가 발생했습니다", e);
            return EMPTY_CONTEXT_JSON;
        }
    }

    private Map<String, Object> sanitizeContextData(Map<String, Object> rawMap) {
        Map<String, Object> sanitized = new HashMap<>(rawMap);

        // 보안상 위험한 필드 패턴들
        String[] dangerousPatterns = {"password", "secret", "key", "token", "credential"};

        int removedCount = 0;
        var iterator = sanitized.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            String fieldName = entry.getKey().toLowerCase();

            for (String pattern : dangerousPatterns) {
                if (fieldName.contains(pattern)) {
                    log.warn("보안상 위험한 필드를 컨텍스트에서 제거했습니다: {}", entry.getKey());
                    iterator.remove();
                    removedCount++;
                    break;
                }
            }
        }

        if (removedCount > 0) {
            log.info("총 {}개의 민감한 필드를 컨텍스트에서 제거했습니다", removedCount);
        }

        return sanitized;
    }

    private String createFallbackContextJson(ContextData contextData, Exception error) {
        try {
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("_conversion_error", error.getMessage());
            fallback.put("_original_field_count", contextData.getData().size());
            fallback.put("_timestamp", System.currentTimeMillis());

            // 기본 시스템 정보는 문자열로 보존 시도
            contextData.getData().forEach((key, value) -> {
                if (key.startsWith("thread") || key.startsWith("host")) {
                    try {
                        fallback.put(key, String.valueOf(value));
                    } catch (Exception e) {
                        log.debug("시스템 필드 보존에 실패했습니다: {}", key);
                    }
                }
            });

            log.warn("컨텍스트 변환 실패로 대체 JSON을 생성했습니다: {} 필드", fallback.size());

            return objectMapper.writeValueAsString(fallback);

        } catch (JsonProcessingException fallbackError) {
            log.error("대체 컨텍스트 JSON 생성도 실패했습니다", fallbackError);
            return "{\"_error\":\"complete_context_conversion_failure\"}";
        }
    }

    private ContextData handleContextConversionFailure(String dbValue, Exception error) {
        log.warn("컨텍스트 데이터 변환에 실패했습니다. 부분 복구를 시도합니다");

        // 부분 복구 시도
        try {
            // 단순한 key=value 형태라면 파싱 시도
            if (dbValue.contains("=") && !dbValue.contains("{")) {
                ContextData recovered = parseSimpleKeyValueFormat(dbValue);
                if (!recovered.isEmpty()) {
                    log.info("레거시 key=value 포맷을 성공적으로 복구했습니다");
                    return recovered;
                }
            }
        } catch (Exception recoveryError) {
            log.debug("컨텍스트 복구 시도가 실패했습니다", recoveryError);
        }

        // 복구 불가능한 경우 빈 컨텍스트 반환
        log.warn("컨텍스트 데이터를 복구할 수 없어 빈 컨텍스트를 반환합니다");
        return ContextData.empty();
    }

    private ContextData parseSimpleKeyValueFormat(String dbValue) {
        Map<String, Object> parsed = new HashMap<>();
        String[] pairs = dbValue.split(",");

        for (String pair : pairs) {
            String[] keyValue = pair.trim().split("=", 2);
            if (keyValue.length == 2) {
                parsed.put(keyValue[0].trim(), keyValue[1].trim());
            }
        }

        if (!parsed.isEmpty()) {
            log.info("레거시 컨텍스트 포맷을 성공적으로 파싱했습니다: {} 필드", parsed.size());
            return ContextData.of(parsed);
        }

        return ContextData.empty();
    }

    private boolean isBlankOrEmpty(String value) {
        return value == null || value.trim().isEmpty() || "null".equals(value);
    }
}