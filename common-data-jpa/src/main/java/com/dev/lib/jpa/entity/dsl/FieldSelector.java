package com.dev.lib.jpa.entity.dsl;

import com.dev.lib.jpa.entity.NestedSelectBuilder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class FieldSelector<T> {

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
    public static <T, R> FieldSelector<T> of(SFunction<T, R> fn) {
        List<SFunction<?, ?>> chain = new ArrayList<>();
        chain.add(fn);
        return new FieldSelector<>(fn.getFieldName(), getReturnType(fn), chain);
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
        try {
            String methodName = fn.getSerializedLambda().getImplMethodName();
            Class<?> entityClass = fn.getEntityClass();
            return entityClass.getMethod(methodName).getReturnType();
        } catch (Exception e) {
            return Object.class;
        }
    }
}
