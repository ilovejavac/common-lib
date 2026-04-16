package com.dev.lib.jpa.entity.delete;

import com.dev.lib.jpa.entity.BaseRepositoryImpl;
import com.dev.lib.jpa.entity.JpaEntity;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.PathBuilder;
import jakarta.persistence.CascadeType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import org.hibernate.Hibernate;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class CascadeFieldResolver {

    private static final Map<Class<?>, List<Field>> CASCADE_FIELDS_CACHE = new ConcurrentHashMap<>(128);

    private static final Map<Class<?>, PathBuilder<?>> PATH_BUILDER_CACHE = new ConcurrentHashMap<>(128);

    private CascadeFieldResolver() {
    }

    public static <T extends JpaEntity> boolean hasCascadeFields(BaseRepositoryImpl<T> repository) {
        return !getCascadeFields(repository.getPath().getType()).isEmpty();
    }

    public static List<Field> getCascadeFields(Class<?> clazz) {
        return CASCADE_FIELDS_CACHE.computeIfAbsent(clazz, CascadeFieldResolver::resolveCascadeFields);
    }

    public static PathBuilder<?> createPathBuilder(Class<?> clazz) {
        return PATH_BUILDER_CACHE.computeIfAbsent(clazz, CascadeFieldResolver::newPathBuilder);
    }

    /**
     * Collect all cascade-reachable entities from roots into a type→ids map.
     * The map is ordered parent-first (insertion order).
     * Skips entities already marked as deleted.
     */
    public static void collectCascadeEntities(Object entity, Set<Object> visited, Map<Class<?>, Set<Long>> toDeleteByType) {
        if (entity == null || visited.contains(entity)) {
            return;
        }
        visited.add(entity);

        Class<?> realClass = Hibernate.getClass(entity);

        if (entity instanceof JpaEntity jpaEntity) {
            if (Boolean.TRUE.equals(jpaEntity.getDeleted())) {
                return;
            }
            Long id = jpaEntity.getId();
            if (id != null) {
                toDeleteByType
                        .computeIfAbsent(realClass, key -> new LinkedHashSet<>())
                        .add(id);
            }
        }

        List<Field> cascadeFields = getCascadeFields(realClass);
        for (Field field : cascadeFields) {
            Object value = ReflectionUtils.getField(field, entity);
            if (!Hibernate.isInitialized(value)) {
                Hibernate.initialize(value);
            }
            if (value instanceof Collection<?> collection) {
                for (Object item : collection) {
                    collectCascadeEntities(item, visited, toDeleteByType);
                }
            } else if (value != null) {
                collectCascadeEntities(value, visited, toDeleteByType);
            }
        }
    }

    /**
     * Same as collectCascadeEntities but for hard delete — does NOT skip deleted entities.
     */
    public static void collectCascadeEntitiesIncludeDeleted(Object entity, Set<Object> visited, Map<Class<?>, Set<Long>> toDeleteByType) {
        if (entity == null || visited.contains(entity)) {
            return;
        }
        visited.add(entity);

        Class<?> realClass = Hibernate.getClass(entity);

        if (entity instanceof JpaEntity jpaEntity) {
            Long id = jpaEntity.getId();
            if (id != null) {
                toDeleteByType
                        .computeIfAbsent(realClass, key -> new LinkedHashSet<>())
                        .add(id);
            }
        }

        List<Field> cascadeFields = getCascadeFields(realClass);
        for (Field field : cascadeFields) {
            Object value = ReflectionUtils.getField(field, entity);
            if (!Hibernate.isInitialized(value)) {
                Hibernate.initialize(value);
            }
            if (value instanceof Collection<?> collection) {
                for (Object item : collection) {
                    collectCascadeEntitiesIncludeDeleted(item, visited, toDeleteByType);
                }
            } else if (value != null) {
                collectCascadeEntitiesIncludeDeleted(value, visited, toDeleteByType);
            }
        }
    }

    /**
     * Execute a batch operation (DELETE or UPDATE) for each type in the map.
     * @param entries the type→ids entries in the order they should be processed
     * @param batchSize max IDs per IN clause
     * @param action callback receiving (PathBuilder, NumberPath<Long> idPath, List<Long> idBatch) for each sub-batch
     */
    public static void executeByType(
            List<Map.Entry<Class<?>, Set<Long>>> entries,
            int batchSize,
            TypeBatchAction action
    ) {
        for (Map.Entry<Class<?>, Set<Long>> entry : entries) {
            Class<?> clazz = entry.getKey();
            List<Long> ids = new ArrayList<>(entry.getValue());
            if (ids.isEmpty()) {
                continue;
            }

            PathBuilder<?> builder = createPathBuilder(clazz);
            NumberPath<Long> builderIdPath = builder.getNumber("id", Long.class);

            for (int i = 0; i < ids.size(); i += batchSize) {
                List<Long> batch = ids.subList(i, Math.min(i + batchSize, ids.size()));
                action.execute(builder, builderIdPath, batch);
            }
        }
    }

    @FunctionalInterface
    public interface TypeBatchAction {
        void execute(PathBuilder<?> builder, NumberPath<Long> idPath, List<Long> batch);
    }

    /**
     * Create a new type→ids map (LinkedHashMap to preserve insertion order).
     */
    public static Map<Class<?>, Set<Long>> newDeleteByTypeMap() {
        return new LinkedHashMap<>();
    }

    /**
     * Create a new identity-based visited set for cycle detection.
     */
    public static Set<Object> newVisitedSet() {
        return java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    }

    private static List<Field> resolveCascadeFields(Class<?> clazz) {
        List<Field> result = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (shouldCascadeRemove(field)) {
                    ReflectionUtils.makeAccessible(field);
                    result.add(field);
                }
            }
            current = current.getSuperclass();
        }
        return result;
    }

    private static boolean shouldCascadeRemove(Field field) {
        OneToMany oneToMany = field.getAnnotation(OneToMany.class);
        if (oneToMany != null && (hasCascadeRemove(oneToMany.cascade()) || oneToMany.orphanRemoval())) {
            return true;
        }
        OneToOne oneToOne = field.getAnnotation(OneToOne.class);
        return oneToOne != null && (hasCascadeRemove(oneToOne.cascade()) || oneToOne.orphanRemoval());
    }

    private static boolean hasCascadeRemove(CascadeType[] cascadeTypes) {
        for (CascadeType type : cascadeTypes) {
            if (type == CascadeType.REMOVE || type == CascadeType.ALL) {
                return true;
            }
        }
        return false;
    }

    private static PathBuilder<?> newPathBuilder(Class<?> clazz) {
        String entityName = clazz.getSimpleName();
        String variableName = Character.toLowerCase(entityName.charAt(0)) + entityName.substring(1);
        return new PathBuilder<>(clazz, variableName);
    }
}
