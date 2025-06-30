package com.pandaterry.concurrent_entity_change_logger.entity_change_logging.domain.vo;

import lombok.Value;

import java.lang.reflect.Field;
import java.util.Objects;

@Value
public class EntityIdentifier {
    private final String entityName;
    private final String entityId;

    private EntityIdentifier(String entityName, String entityId) {
        this.entityName = entityName;
        this.entityId = entityId;
    }

    public static EntityIdentifier of(String entityName, String entityId) {
        validateParameters(entityName, entityId);
        return new EntityIdentifier(entityName, entityId);
    }

    public static EntityIdentifier fromEntity(Object entity) {
        Objects.requireNonNull(entity, "Entity cannot be null");

        String entityName = entity.getClass().getSimpleName();
        String entityId = extractEntityId(entity);

        return of(entityName, entityId);
    }

    public boolean represents(Class<?> entityClass) {
        return entityClass.getSimpleName().equals(this.entityName);
    }

    public boolean isSameEntity(EntityIdentifier other) {
        return this.equals(other);
    }

    public boolean isSameEntityType(EntityIdentifier other) {
        return Objects.equals(this.entityName, other.entityName);
    }

    private static void validateParameters(String entityName, String entityId) {
        if (isBlank(entityName)) {
            throw new IllegalArgumentException("Entity name cannot be blank");
        }
        if (isBlank(entityId)) {
            throw new IllegalArgumentException("Entity ID cannot be blank");
        }
    }

    private static String extractEntityId(Object entity) {
        // @Id 어노테이션이 붙은 필드를 찾아서 ID 추출
        Class<?> clazz = entity.getClass();

        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(jakarta.persistence.Id.class)) {
                field.setAccessible(true);
                try {
                    Object idValue = field.get(entity);
                    return idValue != null ? idValue.toString() : "null";
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Failed to extract entity ID", e);
                }
            }
        }

        throw new IllegalArgumentException("Entity must have @Id annotated field: " + entity.getClass());
    }

    private static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    public String getEntityName() { return entityName; }
    public String getEntityId() { return entityId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EntityIdentifier that)) return false;
        return Objects.equals(entityName, that.entityName) &&
                Objects.equals(entityId, that.entityId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entityName, entityId);
    }

    @Override
    public String toString() {
        return String.format("%s[%s]", entityName, entityId);
    }
}