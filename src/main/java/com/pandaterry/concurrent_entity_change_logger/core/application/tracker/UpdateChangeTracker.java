package com.pandaterry.concurrent_entity_change_logger.core.application.tracker;

import com.pandaterry.concurrent_entity_change_logger.core.domain.enumerated.OperationType;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

@Component
public class UpdateChangeTracker extends AbstractChangeTracker {
    @Override
    public Map<String, String> trackChanges(Object oldEntity, Object newEntity) {
        Map<String, String> changes = new HashMap<>();
        Field[] fields = newEntity.getClass().getDeclaredFields();

        for (Field field : fields) {
            if (shouldSkipField(field))
                continue;

            field.setAccessible(true);
            Object oldValue = getFieldValue(field, oldEntity);
            Object newValue = getFieldValue(field, newEntity);

            if (isValueChanged(oldValue, newValue)) {
                changes.put(field.getName(), formatChange(oldValue, newValue));
            }
        }
        return changes;
    }

    @Override
    public boolean supports(OperationType operationType) {
        return operationType == OperationType.UPDATE;
    }
}