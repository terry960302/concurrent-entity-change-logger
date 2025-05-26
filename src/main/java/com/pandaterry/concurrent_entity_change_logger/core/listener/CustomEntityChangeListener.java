package com.pandaterry.concurrent_entity_change_logger.core.listener;

import org.hibernate.event.spi.*;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pandaterry.concurrent_entity_change_logger.core.annotation.ExcludeFromLogging;
import com.pandaterry.concurrent_entity_change_logger.core.config.EntityLoggingConfig;
import com.pandaterry.concurrent_entity_change_logger.core.service.EntityChangeLogger;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

@Component
public class CustomEntityChangeListener implements PostInsertEventListener, PostUpdateEventListener, PostDeleteEventListener {

    @Autowired
    private EntityChangeLogger logger;

    @Autowired
    private EntityLoggingConfig config;

    @Override
    public void onPostInsert(PostInsertEvent event) {
        if (shouldLogChanges(event.getEntity())) {
            logger.logChange(null, event.getEntity(), "INSERT");
        }
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        if (shouldLogChanges(event.getEntity())) {
            Object oldEntity = getOldEntity(event);
            logger.logChange(oldEntity, event.getEntity(), "UPDATE");
        }
    }

    @Override
    public void onPostDelete(PostDeleteEvent event) {
        if (shouldLogChanges(event.getEntity())) {
            logger.logChange(event.getEntity(), null, "DELETE");
        }
    }

    private boolean shouldLogChanges(Object entity) {
        if (!config.isEnableGlobalLogging()) {
            return false;
        }

        String entityName = entity.getClass().getSimpleName();
        if (config.getExcludedEntities().contains(entityName)) {
            return false;
        }

        return !entity.getClass().isAnnotationPresent(ExcludeFromLogging.class);
    }

    private Object getOldEntity(PostUpdateEvent event) {
        EntityPersister persister = event.getPersister();
        Object[] oldState = event.getOldState();
        String[] propertyNames = persister.getPropertyNames();

        try {
            // 엔티티의 새 인스턴스 생성
            Object oldEntity = createEntityInstance(event.getEntity().getClass());

            // 각 프로퍼티의 이전 값 설정
            for (int i = 0; i < propertyNames.length; i++) {
                String propertyName = propertyNames[i];
                setFieldValue(oldEntity, propertyName, oldState[i]);
            }

            return oldEntity;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Object createEntityInstance(Class<?> entityClass) throws Exception {
        Constructor<?> constructor = entityClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }
    private void setFieldValue(Object entity, String fieldName, Object value) throws Exception {
        Field field = findField(entity.getClass(), fieldName);
        if (field != null) {
            field.setAccessible(true);
            field.set(entity, value);
        } else {
            // 필드를 찾지 못한 경우 setter 메서드 시도
            Method setter = findSetter(entity.getClass(), fieldName);
            if (setter != null) {
                setter.invoke(entity, value);
            }
        }
    }

    private Field findField(Class<?> clazz, String fieldName) {
        Class<?> currentClass = clazz;
        while (currentClass != null) {
            try {
                return currentClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                currentClass = currentClass.getSuperclass();
            }
        }
        return null;
    }

    private Method findSetter(Class<?> clazz, String fieldName) {
        String setterName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        return Arrays.stream(clazz.getMethods())
                .filter(method -> method.getName().equals(setterName) && method.getParameterCount() == 1)
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean requiresPostCommitHandling(EntityPersister persister) {
        return false;
    }
}
