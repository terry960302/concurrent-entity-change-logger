package com.pandaterry.concurrent_entity_change_logger.core.util;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EntityStateCopier {

    public Object cloneEntity(PostUpdateEvent event) {
        EntityPersister persister = event.getPersister();
        Object[] oldState = event.getOldState();
        String[] propertyNames = persister.getPropertyNames();

        try {
            Object oldEntity = createEntityInstance(event.getEntity().getClass());
            for (int i = 0; i < propertyNames.length; i++) {
                String propertyName = propertyNames[i];
                setFieldValue(oldEntity, propertyName, oldState[i]);
            }
            return oldEntity;
        } catch (Exception e) {
            log.error("Failed to clone entity", e);
            return null;
        }
    }

    private Object createEntityInstance(Class<?> entityClass) throws Exception {
        return entityClass.getDeclaredConstructor().newInstance();
    }

    private void setFieldValue(Object entity, String fieldName, Object value) throws Exception {
        try {
            var field = entity.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(entity, value);
        } catch (NoSuchFieldException e) {
            // 필드를 찾지 못한 경우 무시
        }
    }
}