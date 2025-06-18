package com.pandaterry.concurrent_entity_change_logger.core.application.tracker;

import com.pandaterry.concurrent_entity_change_logger.core.domain.Operation;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

@Component
public class InsertChangeTracker extends AbstractChangeTracker {
    @Override
    public Map<String, String> trackChanges(Object oldEntity, Object newEntity) {
        Map<String, String> changes = new HashMap<>();
        Field[] fields = newEntity.getClass().getDeclaredFields();

        for (Field field : fields) {
            if (shouldSkipField(field))
                continue;

            field.setAccessible(true);
            Object value = getFieldValue(field, newEntity);
            if (value != null) {
                changes.put(field.getName(), value.toString());
            }
        }
        return changes;
    }

    @Override
    public boolean supports(Operation operationType) {
        return operationType == Operation.CREATE;
    }
}