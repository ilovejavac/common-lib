package com.dev.lib.jpa.entity;

import com.dev.lib.jpa.entity.dsl.FieldSelector;
import com.dev.lib.jpa.entity.dsl.SFunction;

import java.util.ArrayList;
import java.util.List;

/**
 * 嵌套字段选择构建器
 * 用法: NestedSelectBuilder.of(User::getProfile).then(Profile::getName)
 */
public class NestedSelectBuilder<ROOT, CURRENT> {

    private final Class<ROOT>           rootClass;
    private final List<SFunction<?, ?>> chain = new ArrayList<>();
    private String                      currentPath;

    private NestedSelectBuilder(Class<ROOT> rootClass, SFunction<ROOT, CURRENT> first) {
        this.rootClass = rootClass;
        this.chain.add(first);
        this.currentPath = first.getFieldName();
    }

    public static <T, R> NestedSelectBuilder<T, R> of(SFunction<T, R> fn) {
        return new NestedSelectBuilder<>(fn.getEntityClass(), fn);
    }

    @SuppressWarnings("unchecked")
    public <NEXT> NestedSelectBuilder<ROOT, NEXT> then(SFunction<CURRENT, NEXT> fn) {
        NestedSelectBuilder<ROOT, NEXT> next = new NestedSelectBuilder<>(
                this.rootClass, 
                (SFunction<ROOT, NEXT>) this.chain.get(0)
        );
        next.chain.clear();
        next.chain.addAll(this.chain);
        next.chain.add(fn);
        next.currentPath = this.currentPath + "." + fn.getFieldName();
        return next;
    }

    public FieldSelector<ROOT> build() {
        return new FieldSelector<>(currentPath, resolveType(), new ArrayList<>(chain));
    }

    private Class<?> resolveType() {
        try {
            SFunction<?, ?> last = chain.get(chain.size() - 1);
            String methodName = last.getSerializedLambda().getImplMethodName();
            return last.getEntityClass().getMethod(methodName).getReturnType();
        } catch (Exception e) {
            return Object.class;
        }
    }
}
