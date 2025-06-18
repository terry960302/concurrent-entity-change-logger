package com.pandaterry.concurrent_entity_change_logger.core.infrastructure.persistence;

import com.pandaterry.concurrent_entity_change_logger.core.domain.LogEntry;
import com.pandaterry.concurrent_entity_change_logger.core.application.tracker.EntityChangeTracker;
import com.pandaterry.concurrent_entity_change_logger.core.domain.Operation;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.Optional;

@Component
public class LogEntryFactory {
    private final EntityChangeTracker changeTracker;

    public LogEntryFactory(EntityChangeTracker changeTracker) {
        this.changeTracker = changeTracker;
    }

    public LogEntry create(Object oldEntity, Object newEntity, Operation operation) {
        String entityName = getEntityName(oldEntity, newEntity);
        String entityId = getEntityId(oldEntity, newEntity);
        var changes = changeTracker.trackChanges(oldEntity, newEntity, operation);

        return LogEntry.builder()
                .entityName(entityName)
                .entityId(entityId)
                .operation(operation)
                .changes(changes)
                .build();
    }

    private String getEntityName(Object oldEntity, Object newEntity) {
        return (newEntity != null) ? newEntity.getClass().getSimpleName()
                : oldEntity.getClass().getSimpleName();
    }

    private String getEntityId(Object oldEntity, Object newEntity) {
        Object entity = newEntity != null ? newEntity : oldEntity;
        Class<?> clazz = entity.getClass();
        Optional<Field> idField = findIdField(clazz);

        if (idField.isPresent()) {
            Field field = idField.get();
            field.setAccessible(true);
            if (!field.canAccess(entity)) {
                return "Access Error";
            }
            Object idValue = getFieldValue(field, entity);
            return idValue != null ? idValue.toString() : "null";
        }
        return "Unknown";
    }

    private Object getFieldValue(Field field, Object entity) {
        try {
            return field.get(entity);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    private Optional<Field> findIdField(Class<?> clazz) {
        Optional<Field> idField = java.util.Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(jakarta.persistence.Id.class))
                .findFirst();

        if (!idField.isPresent() && clazz.getSuperclass() != null) {
            return findIdField(clazz.getSuperclass());
        }
        return idField;
    }
}