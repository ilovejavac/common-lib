package com.dev.lib.entity.dsl;

import com.dev.lib.entity.dsl.core.LambdaFieldNameResolver;

import java.io.Serializable;
import java.util.function.Function;

@FunctionalInterface
public interface QueryRef<T, R> extends Function<T, R>, Serializable {

    default String getFieldName() {

        return LambdaFieldNameResolver.resolveFieldName(this);
    }
}
