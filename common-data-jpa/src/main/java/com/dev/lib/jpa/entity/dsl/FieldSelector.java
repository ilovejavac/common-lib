package com.dev.lib.jpa.entity.dsl;

import com.dev.lib.jpa.entity.NestedSelectBuilder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class FieldSelector<T> {

    private static final Map<SFunction<?, ?>, Class<?>> RETURN_TYPE_CACHE = new ConcurrentHashMap<>();

    private final String path;
    private final Class<?> fieldType;
    private final List<SFunction<?, ?>> chain;

    public FieldSelector(String path, Class<?> fieldType, List<SFunction<?, ?>> chain) {
        this.path = path;
        this.fieldType = fieldType;
        this.chain = chain;
    }

    /**
     * 简单字段: User::getName
     */
    public static <T, R> FieldSelector<T> of(SFunction<? super T, R> fn) {
        List<SFunction<?, ?>> chain = new ArrayList<>();
        chain.add((SFunction<?, ?>) fn);
        return new FieldSelector<>(fn.getFieldName(), getReturnType((SFunction<?, ?>) fn), chain);
    }

    /**
     * 嵌套字段: of(User::getProfile).then(Profile::getName)
     */
    public <R> FieldSelector<T> then(SFunction<?, R> fn) {
        List<SFunction<?, ?>> newChain = new ArrayList<>(this.chain);
        newChain.add(fn);
        String newPath = this.path + "." + fn.getFieldName();
        return new FieldSelector<>(newPath, getReturnType(fn), newChain);
    }

    public static <T, R> NestedSelectBuilder<T, R> nested(SFunction<T, R> fn) {
        return NestedSelectBuilder.of(fn);
    }
    private static Class<?> getReturnType(SFunction<?, ?> fn) {
        return RETURN_TYPE_CACHE.computeIfAbsent(fn, f -> {
            try {
                String methodName = f.getSerializedLambda().getImplMethodName();
                Class<?> entityClass = f.getEntityClass();
                return entityClass.getMethod(methodName).getReturnType();
            } catch (Exception e) {
                return Object.class;
            }
        });
    }
}
