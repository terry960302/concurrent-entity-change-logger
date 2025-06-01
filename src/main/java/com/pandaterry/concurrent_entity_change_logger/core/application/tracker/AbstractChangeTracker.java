package com.pandaterry.concurrent_entity_change_logger.core.application.tracker;

import com.pandaterry.concurrent_entity_change_logger.core.shared.annotation.ExcludeFromLogging;

import java.lang.reflect.Field;
import java.util.Objects;

public abstract class AbstractChangeTracker implements ChangeTracker {
    protected Object getFieldValue(Field field, Object entity) {
        try {
            return field.get(entity);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    protected boolean shouldSkipField(Field field) {
        return field.isAnnotationPresent(jakarta.persistence.Transient.class) ||
                field.isAnnotationPresent(ExcludeFromLogging.class);
    }

    protected String formatChange(Object oldValue, Object newValue) {
        return String.format("%s -> %s",
                oldValue != null ? oldValue.toString() : "null",
                newValue != null ? newValue.toString() : "null");
    }

    protected boolean isValueChanged(Object oldValue, Object newValue) {
        return !Objects.equals(oldValue, newValue);
    }
}