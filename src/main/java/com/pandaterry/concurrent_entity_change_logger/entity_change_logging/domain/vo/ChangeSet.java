package com.pandaterry.concurrent_entity_change_logger.entity_change_logging.domain.vo;

import lombok.Value;

import java.util.*;

@Value
public class ChangeSet {
    private static final int MAX_CHANGES = 100;
    private static final Set<String> CRITICAL_FIELDS = Set.of("password", "email", "status");

    private final Map<String, FieldChange> changes;

    private ChangeSet(Map<String, FieldChange> changes) {
        this.changes = Map.copyOf(changes);
    }

    public static ChangeSet empty() {
        return new ChangeSet(Map.of());
    }

    public static ChangeSet of(Map<String, FieldChange> changes) {
        Objects.requireNonNull(changes, "changes 는 비어있을 수 없습니다.");
        if (changes.size() > MAX_CHANGES) {
            throw new IllegalArgumentException("changes 가 너무 많습니다: " + changes.size());
        }
        return new ChangeSet(changes);
    }

    public boolean hasAnyChanges() {
        return !changes.isEmpty();
    }

    public boolean hasBusinessImpactingChanges() {
        return changes.values().stream()
                .anyMatch(FieldChange::isBusinessImpacting);
    }

    public boolean containsCriticalFields() {
        return changes.keySet().stream()
                .anyMatch(CRITICAL_FIELDS::contains);
    }

    public boolean exceedsMaximumSize() {
        return changes.size() > MAX_CHANGES;
    }

    public ChangeSet enrichWith(Map<String, Object> additionalContext) {
        Map<String, FieldChange> enrichedChanges = new HashMap<>(changes);
        additionalContext.forEach((key, value) ->
                enrichedChanges.put("ctx_" + key, FieldChange.contextual(key, value)));
        return new ChangeSet(enrichedChanges);
    }

    public int getChangeCount() {
        return changes.size();
    }

    public Map<String, FieldChange> getChanges() {
        return changes;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> simpleMap = new HashMap<>();

        this.changes.forEach((fieldName, fieldChange) -> {
            Map<String, Object> changeData = new HashMap<>();
            changeData.put("fieldName", fieldChange.getFieldName());
            changeData.put("oldValue", extractSimpleValue(fieldChange.getOldValue()));
            changeData.put("newValue", extractSimpleValue(fieldChange.getNewValue()));

            changeData.put("changeType", fieldChange.getChangeType().name());

            simpleMap.put(fieldName, changeData);
        });

        return simpleMap;
    }

    private static Object extractSimpleValue(Object value) {
        if (value == null) {
            return null;
        }

        // FieldChange 객체가 중첩된 경우 실제 값만 추출
        if (value instanceof FieldChange nestedFieldChange) {
            return extractSimpleValue(nestedFieldChange.getNewValue());
        }

        // FieldChange toString() 결과인 경우 파싱
        String stringValue = value.toString();
        if (stringValue.startsWith("{") && stringValue.contains("newValue=")) {
            return parseFieldChangeString(stringValue);
        }

        return value;
    }

    private static Object parseFieldChangeString(String fieldChangeString) {
        try {
            // "{newValue=47, fieldName=id, changeType=CREATED, oldValue=null}" 형태 파싱
            String newValuePart = fieldChangeString.substring(fieldChangeString.indexOf("newValue=") + 9);
            String value = newValuePart.substring(0, newValuePart.indexOf(",")).trim();

            // "null" 문자열을 실제 null로 변환
            return "null".equals(value) ? null : value;
        } catch (Exception e) {
            // 파싱 실패시 원본 반환
            return fieldChangeString;
        }
    }

    public static ChangeSet from(Map<String, Object> rawChanges) {
        if (rawChanges == null || rawChanges.isEmpty()) {
            return ChangeSet.empty();
        }

        Map<String, FieldChange> processedChanges = new HashMap<>();

        rawChanges.forEach((fieldName, fieldValue) -> {
            FieldChange fieldChange = convertToFieldChange(fieldName, fieldValue);
            if (fieldChange != null && fieldChange.isSignificantChange()) {
                processedChanges.put(fieldName, fieldChange);
            }
        });

        return ChangeSet.of(processedChanges);
    }

    // 기존 tracker 결과를 FieldChange로 변환하는 로직
    private static FieldChange convertToFieldChange(String fieldName, Object fieldValue) {
        if (fieldValue == null) {
            return null;
        }

        String stringValue = fieldValue.toString().trim();

        // UpdateChangeTracker 결과 처리: "oldValue -> newValue" 형태
        if (stringValue.contains(" -> ")) {
            return parseUpdateChange(fieldName, stringValue);
        }

        // InsertChangeTracker 결과 처리: 단순 값 (새로 생성)
        // DeleteChangeTracker 결과 처리: 단순 값 (삭제된 값)
        return parseSimpleChange(fieldName, stringValue);
    }

    // UpdateChangeTracker의 "oldValue -> newValue" 형태 파싱
    private static FieldChange parseUpdateChange(String fieldName, String changeValue) {
        String[] parts = changeValue.split(" -> ", 2);
        if (parts.length != 2) {
            // 파싱 실패시 수정된 것으로 간주
            return FieldChange.of(fieldName, null, changeValue);
        }

        String oldValueStr = parts[0].trim();
        String newValueStr = parts[1].trim();

        // "null" 문자열을 실제 null로 변환
        Object oldValue = "null".equals(oldValueStr) ? null : oldValueStr;
        Object newValue = "null".equals(newValueStr) ? null : newValueStr;

        // 실제 변경이 없는 경우 null 반환
        if (Objects.equals(oldValue, newValue)) {
            return null;
        }

        return FieldChange.of(fieldName, oldValue, newValue);
    }

    // Insert/Delete Tracker의 단순 값 파싱
    private static FieldChange parseSimpleChange(String fieldName, String value) {
        // 빈 문자열이나 "null" 제외
        if (value.isEmpty() || "null".equals(value)) {
            return null;
        }

        // Insert/Delete 구분은 Operation에 따라 결정되므로
        // 여기서는 값이 존재한다고 가정하고 CREATED로 처리
        // (실제로는 Operation 컨텍스트가 필요하지만, 기존 구조 유지)
        return FieldChange.of(fieldName, null, value);
    }
}
