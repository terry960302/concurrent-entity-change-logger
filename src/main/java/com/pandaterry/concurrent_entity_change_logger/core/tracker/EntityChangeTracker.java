package com.pandaterry.concurrent_entity_change_logger.core.tracker;

import com.pandaterry.concurrent_entity_change_logger.core.annotation.ExcludeFromLogging;
import com.pandaterry.concurrent_entity_change_logger.core.enumerated.OperationType;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Component
public class EntityChangeTracker {
    public Map<String, String> trackChanges(Object oldEntity, Object newEntity, OperationType operation) {
        if (operation == OperationType.UPDATE && oldEntity != null && newEntity != null) {
            return trackUpdateChanges(oldEntity, newEntity);
        } else if (operation == OperationType.INSERT && newEntity != null) {
            return trackInsertChanges(newEntity);
        } else if (operation == OperationType.DELETE && oldEntity != null) {
            return trackDeleteChanges(oldEntity);
        }
        return new HashMap<>();
    }

    private Map<String, String> trackUpdateChanges(Object oldEntity, Object newEntity) {
        Map<String, String> changes = new HashMap<>();
        Field[] fields = newEntity.getClass().getDeclaredFields();

        for (Field field : fields) {
            if (shouldSkipField(field))
                continue;

            field.setAccessible(true);
            Object oldValue = getFieldValue(field, oldEntity);
            Object newValue = getFieldValue(field, newEntity);

            if (!Objects.equals(oldValue, newValue)) {
                changes.put(field.getName(), formatChange(oldValue, newValue));
            }
        }
        return changes;
    }

    private Map<String, String> trackInsertChanges(Object entity) {
        Map<String, String> changes = new HashMap<>();
        Field[] fields = entity.getClass().getDeclaredFields();

        for (Field field : fields) {
            if (shouldSkipField(field))
                continue;

            field.setAccessible(true);
            Object value = getFieldValue(field, entity);
            if (value != null) {
                changes.put(field.getName(), value.toString());
            }
        }
        return changes;
    }

    private Map<String, String> trackDeleteChanges(Object entity) {
        Map<String, String> changes = new HashMap<>();
        Field[] fields = entity.getClass().getDeclaredFields();

        for (Field field : fields) {
            if (shouldSkipField(field))
                continue;

            field.setAccessible(true);
            Object value = getFieldValue(field, entity);
            if (value != null) {
                changes.put(field.getName(), value.toString());
            }
        }
        return changes;
    }

    private Object getFieldValue(Field field, Object entity) {
        try {
            return field.get(entity);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    private boolean shouldSkipField(Field field) {
        return field.isAnnotationPresent(jakarta.persistence.Transient.class) ||
                field.isAnnotationPresent(ExcludeFromLogging.class);
    }

    private String formatChange(Object oldValue, Object newValue) {
        return String.format("%s -> %s",
                oldValue != null ? oldValue.toString() : "null",
                newValue != null ? newValue.toString() : "null");
    }
}