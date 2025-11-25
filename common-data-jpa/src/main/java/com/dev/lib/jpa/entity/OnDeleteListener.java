package com.dev.lib.jpa.entity;

import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PreRemove;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class OnDeleteListener {

    private boolean isNullifyField(Field field) {
        return field.isAnnotationPresent(OnDeleteSetNull.class)
                && (field.isAnnotationPresent(OneToMany.class)
                || field.isAnnotationPresent(ManyToMany.class));
    }

    @PreRemove
    public void preRemove(Object entity) {
        for (Field field : entity.getClass().getDeclaredFields()) {
            if (isNullifyField(field)) {
                processNullifyField(field, entity);
            }
        }
    }

    private void processNullifyField(Field field, Object entity) {
        ReflectionUtils.makeAccessible(field);
        try {
            Object value = field.get(entity);
            if (value instanceof Collection<?> children) {
                children.forEach(child -> nullifyParent(child, entity));
            }
        } catch (IllegalAccessException e) {
            log.warn("Failed to access field {} on entity {}", field.getName(), entity.getClass().getName(), e);
        }
    }

    private void nullifyParent(Object child, Object parent) {
        Class<?> parentClass = parent.getClass();

        findMatchingRelationField(child, parentClass).ifPresent(field -> clearRelation(field, child, parent));
    }

    private Optional<Field> findMatchingRelationField(Object child, Class<?> parentClass) {
        return Arrays.stream(child.getClass().getDeclaredFields())
                .filter(this::isRelationField)
                .filter(field -> matchesParentType(field, parentClass))
                .findFirst();
    }

    private boolean isRelationField(Field field) {
        return field.isAnnotationPresent(ManyToOne.class)
                || field.isAnnotationPresent(ManyToMany.class);
    }

    private boolean matchesParentType(Field field, Class<?> parentClass) {
        if (field.isAnnotationPresent(ManyToOne.class)) {
            return field.getType().isAssignableFrom(parentClass);
        }
        return isCollectionOf(field, parentClass);
    }

    private void clearRelation(Field field, Object child, Object parent) {
        ReflectionUtils.makeAccessible(field);
        try {
            Object value = field.get(child);
            if (field.isAnnotationPresent(ManyToOne.class) && Objects.equals(value, parent)) {
                ReflectionUtils.setField(field, child, null);
            } else if (value instanceof Collection<?> c) {
                c.remove(parent);
            }
        } catch (IllegalAccessException e) {
            log.warn(
                    "Failed to clear relation on field {} for child {}",
                    field.getName(),
                    child.getClass().getName(),
                    e
            );
        }
    }

    private boolean isCollectionOf(Field field, Class<?> targetClass) {
        if (field.getGenericType() instanceof ParameterizedType pt) {
            Type[] typeArgs = pt.getActualTypeArguments();
            if (typeArgs.length > 0 && typeArgs[0] instanceof Class<?> c) {
                return c.isAssignableFrom(targetClass);
            }
        }
        return false;
    }
}