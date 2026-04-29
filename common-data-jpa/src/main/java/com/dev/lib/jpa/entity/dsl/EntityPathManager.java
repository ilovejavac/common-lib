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

    private static final Map<Class<?>, EntityPathBase<?>> CACHE = new ConcurrentHashMap<>(128);

    @SuppressWarnings("unchecked")
    public static <E> EntityPathBase<E> getEntityPath(Class<E> entityClass) {

        return (EntityPathBase<E>) CACHE.computeIfAbsent(
                entityClass,
                clazz -> {
                    String qClassName = buildQueryDslClassName(clazz);
                    String fieldName  = Character.toLowerCase(clazz.getSimpleName().charAt(0))
                            + clazz.getSimpleName().substring(1);
                    try {
                        Class<?> qClass = Class.forName(qClassName);
                        Field field = qClass.getDeclaredField(fieldName);
                        ReflectionUtils.makeAccessible(field);

                        return (EntityPathBase<?>) field.get(null);
                    } catch (Exception e) {
                        throw new IllegalStateException(
                                "无法创建 Q 类实例: " + clazz.getName()
                                        + ", qClass=" + qClassName
                                        + ", field=" + fieldName,
                                e
                        );
                    }
                }
        );
    }

    private static String buildQueryDslClassName(Class<?> entityClass) {

        String packageName = entityClass.getPackageName();
        String binaryName  = entityClass.getName();
        String simpleName  = packageName.isEmpty()
                ? binaryName
                : binaryName.substring(packageName.length() + 1);
        String qSimpleName = "Q" + simpleName.replace('$', '_');

        return packageName.isEmpty() ? qSimpleName : packageName + "." + qSimpleName;
    }

}
