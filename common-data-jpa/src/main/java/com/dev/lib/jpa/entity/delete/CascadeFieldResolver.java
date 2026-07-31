package com.dev.lib.jpa.entity.delete;

import com.querydsl.core.types.dsl.PathBuilder;
import jakarta.persistence.CascadeType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class CascadeFieldResolver {

    private static final Map<Class<?>, List<Field>> CASCADE_FIELDS = new ConcurrentHashMap<>(128);

    private static final Map<Class<?>, PathBuilder<?>> PATH_BUILDERS = new ConcurrentHashMap<>(128);

    private CascadeFieldResolver() {
    }

    public static List<Field> getCascadeFields(Class<?> entityClass) {

        return CASCADE_FIELDS.computeIfAbsent(entityClass, CascadeFieldResolver::resolveCascadeFields);
    }

    public static PathBuilder<?> createPathBuilder(Class<?> entityClass) {

        return PATH_BUILDERS.computeIfAbsent(entityClass, CascadeFieldResolver::createPathBuilderInternal);
    }

    private static List<Field> resolveCascadeFields(Class<?> entityClass) {

        List<Field> fields = new ArrayList<>();
        for (Class<?> current = entityClass; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (shouldCascadeRemove(field)) {
                    ReflectionUtils.makeAccessible(field);
                    fields.add(field);
                }
            }
        }
        return List.copyOf(fields);
    }

    private static boolean shouldCascadeRemove(Field field) {

        OneToMany oneToMany = field.getAnnotation(OneToMany.class);
        if (oneToMany != null) {
            return oneToMany.orphanRemoval() || hasCascadeRemove(oneToMany.cascade());
        }

        OneToOne oneToOne = field.getAnnotation(OneToOne.class);
        if (oneToOne != null) {
            return oneToOne.orphanRemoval() || hasCascadeRemove(oneToOne.cascade());
        }

        ManyToMany manyToMany = field.getAnnotation(ManyToMany.class);
        if (manyToMany != null) {
            return hasCascadeRemove(manyToMany.cascade());
        }

        ManyToOne manyToOne = field.getAnnotation(ManyToOne.class);
        return manyToOne != null && hasCascadeRemove(manyToOne.cascade());
    }

    private static boolean hasCascadeRemove(CascadeType[] cascadeTypes) {

        for (CascadeType type : cascadeTypes) {
            if (type == CascadeType.ALL || type == CascadeType.REMOVE) {
                return true;
            }
        }
        return false;
    }

    private static PathBuilder<?> createPathBuilderInternal(Class<?> entityClass) {

        String entityName = entityClass.getSimpleName();
        String variableName = Character.toLowerCase(entityName.charAt(0)) + entityName.substring(1);
        return new PathBuilder<>(entityClass, variableName);
    }
}
