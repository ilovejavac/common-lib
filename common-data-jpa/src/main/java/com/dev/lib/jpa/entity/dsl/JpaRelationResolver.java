package com.dev.lib.jpa.entity.dsl;

import com.dev.lib.entity.dsl.core.RelationInfo;
import com.dev.lib.entity.dsl.core.RelationResolver;
import com.dev.lib.entity.dsl.core.RelationType;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JPA 关联关系解析器
 * <p>
 * 从实体类读取 @OneToMany, @ManyToOne, @OneToOne, @ManyToMany 关联信息
 */
@Component
public class JpaRelationResolver implements RelationResolver {

    private static final Map<String, RelationInfo> CACHE = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // 注册到 Holder
        RelationResolver.Holder.register(this);
    }

    @Override
    public RelationInfo resolve(Class<?> entityClass, String fieldName) {

        if (entityClass == null || fieldName == null || fieldName.isEmpty()) {
            return null;
        }
        String cacheKey = entityClass.getName() + "#" + fieldName;
        return CACHE.computeIfAbsent(cacheKey, k -> doResolve(entityClass, fieldName)
        );
    }

    private RelationInfo doResolve(Class<?> entityClass, String fieldName) {

        Field field = findField(entityClass, fieldName);
        if (field == null) {
            return null;
        }

        RelationType relationType = null;
        String       mappedBy     = null;

        if (field.isAnnotationPresent(OneToMany.class)) {
            OneToMany ann = field.getAnnotation(OneToMany.class);
            relationType = RelationType.ONE_TO_MANY;
            mappedBy = ann.mappedBy();
        } else if (field.isAnnotationPresent(ManyToOne.class)) {
            relationType = RelationType.MANY_TO_ONE;
        } else if (field.isAnnotationPresent(OneToOne.class)) {
            OneToOne ann = field.getAnnotation(OneToOne.class);
            relationType = RelationType.ONE_TO_ONE;
            mappedBy = ann.mappedBy();
        } else if (field.isAnnotationPresent(ManyToMany.class)) {
            ManyToMany ann = field.getAnnotation(ManyToMany.class);
            relationType = RelationType.MANY_TO_MANY;
            mappedBy = ann.mappedBy();
        }

        if (relationType == null) {
            return null;
        }

        Class<?> targetEntity = resolveTargetEntity(field);
        String   joinField    = resolveJoinField(
                entityClass,
                targetEntity,
                mappedBy,
                relationType
        );

        return new RelationInfo(
                relationType,
                targetEntity,
                fieldName,
                joinField,
                mappedBy
        );
    }

    private Class<?> resolveTargetEntity(Field field) {

        Class<?> fieldType = field.getType();

        if (Collection.class.isAssignableFrom(fieldType)) {
            Type genericType = field.getGenericType();
            if (genericType instanceof ParameterizedType pt) {
                Type[] typeArgs = pt.getActualTypeArguments();
                if (typeArgs.length > 0 && typeArgs[0] instanceof Class<?> cls) {
                    return cls;
                }
            }
        }

        return fieldType;
    }

    private String resolveJoinField(
            Class<?> entityClass,
            Class<?> targetEntity,
            String mappedBy,
            RelationType relationType
    ) {

        if (mappedBy != null && !mappedBy.isEmpty()) {
            return mappedBy;
        }

        if (relationType == RelationType.MANY_TO_ONE || relationType == RelationType.ONE_TO_ONE) {
            return findReverseField(targetEntity, entityClass);
        }

        return null;
    }

    private String findReverseField(Class<?> targetEntity, Class<?> sourceEntity) {

        Class<?> current = targetEntity;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (field.getType().equals(sourceEntity)) {
                    return field.getName();
                }
            }
            current = current.getSuperclass();
        }
        return Character.toLowerCase(sourceEntity.getSimpleName().charAt(0))
                + sourceEntity.getSimpleName().substring(1);
    }

    private Field findField(Class<?> clazz, String fieldName) {

        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                Field field = current.getDeclaredField(fieldName);
                ReflectionUtils.makeAccessible(field);
                return field;
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

}