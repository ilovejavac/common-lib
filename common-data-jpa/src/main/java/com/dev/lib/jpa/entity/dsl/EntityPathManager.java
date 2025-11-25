package com.dev.lib.jpa.entity.dsl;

import com.querydsl.core.types.dsl.EntityPathBase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class EntityPathManager {
    private EntityPathManager() {
    }

    private static final Map<Class<?>, EntityPathBase<?>> CACHE = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public static <E> EntityPathBase<E> getEntityPath(Class<E> entityClass) {
        return (EntityPathBase<E>) CACHE.computeIfAbsent(
                entityClass, clazz -> {
                    try {
                        String qClassName = clazz.getPackage().getName() + ".Q" + clazz.getSimpleName();
                        Class<?> qClass = Class.forName(qClassName);

                        String fieldName = Character.toLowerCase(clazz.getSimpleName().charAt(0))
                                + clazz.getSimpleName().substring(1);

                        Field field = qClass.getDeclaredField(fieldName);
                        ReflectionUtils.makeAccessible(field);

                        return (EntityPathBase<?>) field.get(null);
                    } catch (Exception e) {
                        throw new IllegalStateException("无法创建 Q 类实例: " + clazz.getName(), e);
                    }
                }
        );
    }
}