package com.pandaterry.concurrent_entity_change_logger.entity_change_logging.domain.vo;

import lombok.Getter;
import lombok.Value;

import java.util.Objects;

@Value
@Getter
public class FieldChange {
    private final String fieldName;
    private final Object oldValue;
    private final Object newValue;
    private final ChangeType changeType;

    private FieldChange(String fieldName, Object oldValue, Object newValue, ChangeType changeType) {
        this.fieldName = Objects.requireNonNull(fieldName, "Field name cannot be null");
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.changeType = Objects.requireNonNull(changeType, "Change type cannot be null");
    }

    public static FieldChange of(String fieldName, Object oldValue, Object newValue) {
        ChangeType type = determineChangeType(oldValue, newValue);
        return new FieldChange(fieldName, oldValue, newValue, type);
    }

    public static FieldChange contextual(String fieldName, Object value) {
        return new FieldChange(fieldName, null, value, ChangeType.CONTEXTUAL);
    }

    public boolean isBusinessImpacting() {
        return changeType == ChangeType.MODIFIED &&
                !Objects.equals(oldValue, newValue) &&
                (oldValue != null || newValue != null);
    }

    public boolean isSignificantChange() {
        return switch (changeType) {
            case CREATED, DELETED -> true;
            case MODIFIED -> !Objects.equals(oldValue, newValue);
            case CONTEXTUAL -> false;
        };
    }

    public String getFormattedChange() {
        return switch (changeType) {
            case CREATED -> String.format("+ %s", formatValue(newValue));
            case DELETED -> String.format("- %s", formatValue(oldValue));
            case MODIFIED -> String.format("%s → %s", formatValue(oldValue), formatValue(newValue));
            case CONTEXTUAL -> String.format("ctx: %s", formatValue(newValue));
        };
    }

    private static ChangeType determineChangeType(Object oldValue, Object newValue) {
        if (oldValue == null && newValue != null) {
            return ChangeType.CREATED;
        } else if (oldValue != null && newValue == null) {
            return ChangeType.DELETED;
        } else {
            return ChangeType.MODIFIED;
        }
    }

    private String formatValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String && ((String) value).length() > 50) {
            return ((String) value).substring(0, 47) + "...";
        }
        return value.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FieldChange that)) return false;
        return Objects.equals(fieldName, that.fieldName) &&
                Objects.equals(oldValue, that.oldValue) &&
                Objects.equals(newValue, that.newValue) &&
                changeType == that.changeType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fieldName, oldValue, newValue, changeType);
    }

    @Override
    public String toString() {
        return String.format("%s: %s", fieldName, getFormattedChange());
    }

    public enum ChangeType {
        CREATED,    // 새로 생성된 값
        MODIFIED,   // 기존 값이 변경됨
        DELETED,    // 값이 삭제됨
        CONTEXTUAL  // 컨텍스트 정보
    }
}
