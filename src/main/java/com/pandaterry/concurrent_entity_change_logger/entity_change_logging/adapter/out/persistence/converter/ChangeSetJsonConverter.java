package com.pandaterry.concurrent_entity_change_logger.entity_change_logging.adapter.out.persistence.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.domain.vo.ChangeSet;
import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.domain.vo.FieldChange;
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
public class ChangeSetJsonConverter implements AttributeConverter<ChangeSet, String> {

    private final ObjectMapper objectMapper;
    private static final TypeReference<Map<String, Object>> MAP_TYPE_REF =
            new TypeReference<Map<String, Object>>() {
            };

    // 빈 ChangeSet의 JSON 표현 (성능 최적화)
    private static final String EMPTY_CHANGESET_JSON = "{}";

    @Override
    public String convertToDatabaseColumn(ChangeSet changeSet) {
        if (changeSet == null) {
            return EMPTY_CHANGESET_JSON;
        }

        try {
            Map<String, Object> simpleMap = changeSet.toMap();
            String json = objectMapper.writeValueAsString(simpleMap);

            log.debug("Converted ChangeSet to JSON: {} changes -> {} characters",
                    changeSet.getChangeCount(), json.length());

            return json;

        } catch (JsonProcessingException e) {
            log.error("Failed to convert ChangeSet to JSON: {}", changeSet, e);

            // 변환 실패 시 복구 전략: 메타데이터만 저장
            return createFallbackJson(changeSet, e);
        }
    }

    @Override
    public ChangeSet convertToEntityAttribute(String dbValue) {
        if (isBlankOrEmpty(dbValue)) {
            return ChangeSet.empty();
        }

        try {
            Map<String, Object> rawMap = objectMapper.readValue(dbValue, MAP_TYPE_REF);
            Map<String, FieldChange> fieldChanges = convertToFieldChanges(rawMap);

            ChangeSet result = ChangeSet.of(fieldChanges);

            log.debug("Converted JSON to ChangeSet: {} characters -> {} changes",
                    dbValue.length(), result.getChangeCount());

            return result;

        } catch (Exception e) {
            log.error("Failed to convert JSON to ChangeSet: {}", dbValue, e);

            // 변환 실패 시 복구 전략: 빈 ChangeSet 반환
            return handleConversionFailure(dbValue, e);
        }
    }

    private Map<String, FieldChange> convertToFieldChanges(Map<String, Object> rawMap) {
        Map<String, FieldChange> fieldChanges = new HashMap<>();

        rawMap.forEach((fieldName, value) -> {
            try {
                if (value instanceof Map<?, ?> changeData) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> changeMap = (Map<String, Object>) changeData;

                    Object oldValue = changeMap.get("oldValue");
                    Object newValue = changeMap.get("newValue");

                    FieldChange fieldChange = FieldChange.of(fieldName, oldValue, newValue);
                    fieldChanges.put(fieldName, fieldChange);
                } else {
                    // 레거시 포맷 지원: 단순 값인 경우
                    FieldChange fieldChange = FieldChange.of(fieldName, null, value);
                    fieldChanges.put(fieldName, fieldChange);
                }
            } catch (Exception e) {
                log.warn("특정 필드 변환에 실패했습니다.  {}: {}", fieldName, value, e);
                // 개별 필드 변환 실패는 무시하고 계속 진행
            }
        });

        return fieldChanges;
    }

    private String createFallbackJson(ChangeSet changeSet, Exception error) {
        try {
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("_conversion_error", error.getMessage());
            fallback.put("_change_count", changeSet.getChangeCount());
            fallback.put("_has_critical_fields", changeSet.containsCriticalFields());
            fallback.put("_timestamp", System.currentTimeMillis());

            return objectMapper.writeValueAsString(fallback);
        } catch (JsonProcessingException fallbackError) {
            log.error("ChangeSet 변환 실패 Json 저장에 실패했습니다.", fallbackError);
            return "{\"_error\":\"complete_conversion_failure\"}";
        }
    }

    private ChangeSet handleConversionFailure(String dbValue, Exception error) {
        // 에러 메트릭 기록 (향후 모니터링용)
        log.warn("ChangeSet 변환 실패를 모니터링 용도로 기록합니다.");

        // 부분 복구 시도: JSON에서 최소한의 정보 추출
        try {
            if (dbValue.contains("_change_count")) {
                // fallback JSON인 경우 메타데이터 정보 활용
                return ChangeSet.empty(); // 안전한 기본값
            }
        } catch (Exception recoveryError) {
            log.debug("ChangeSet 변환 복구 시도에 실패했습니다.", recoveryError);
        }

        return ChangeSet.empty();
    }

    private boolean isBlankOrEmpty(String value) {
        return value == null || value.trim().isEmpty() || "null".equals(value);
    }
}
