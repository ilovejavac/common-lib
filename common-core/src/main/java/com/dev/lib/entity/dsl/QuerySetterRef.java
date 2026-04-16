package com.dev.lib.entity.dsl;

import com.dev.lib.entity.dsl.core.LambdaFieldNameResolver;

import java.io.Serializable;
import java.util.function.BiConsumer;

@FunctionalInterface
public interface QuerySetterRef<T, V> extends BiConsumer<T, V>, Serializable {

    default String getFieldName() {

        return LambdaFieldNameResolver.resolveFieldName(this);
    }
}
