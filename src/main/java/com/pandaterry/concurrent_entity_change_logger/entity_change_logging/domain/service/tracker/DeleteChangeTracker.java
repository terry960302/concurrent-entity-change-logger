package com.pandaterry.concurrent_entity_change_logger.entity_change_logging.domain.service.tracker;

import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.domain.Operation;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

@Component
public class DeleteChangeTracker extends AbstractChangeTracker {
    @Override
    public Map<String, Object> trackChanges(Object oldEntity, Object newEntity) {
        Map<String, Object> changes = new HashMap<>();
        Field[] fields = oldEntity.getClass().getDeclaredFields();

        for (Field field : fields) {
            if (shouldSkipField(field))
                continue;

            field.setAccessible(true);
            Object value = getFieldValue(field, oldEntity);
            if (value != null) {
                changes.put(field.getName(), value.toString());
            }
        }
        return changes;
    }

    @Override
    public boolean supports(Operation operationType) {
        return operationType == Operation.DELETE;
    }
}