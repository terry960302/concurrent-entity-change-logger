package com.pandaterry.concurrent_entity_change_logger.core.util;

import com.pandaterry.concurrent_entity_change_logger.core.annotation.ExcludeFromLogging;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class EntityLoggingCondition {
    private static boolean enableGlobalLogging = true;
    private static Set<String> excludedEntities = new HashSet<>();

    @Value("${entity.logging.enable-global-logging:true}")
    public void setEnableGlobalLogging(boolean value) {
        enableGlobalLogging = value;
    }

    @Value("${entity.logging.excluded-entities:}")
    public void setExcludedEntities(Set<String> entities) {
        excludedEntities = entities;
    }

    public boolean shouldLogChanges(Object entity) {
        if (!enableGlobalLogging)
            return false;

        String entityName = entity.getClass().getSimpleName();
        if (excludedEntities.contains(entityName)) {
            return false;
        }

        return !entity.getClass().isAnnotationPresent(ExcludeFromLogging.class);
    }
}