package com.dev.lib.entity.dsl.core;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class QueryFieldMerger {

    private QueryFieldMerger() {

    }

    public static List<FieldMetaValue> resolve(Object query) {

        if (query == null) {
            return Collections.emptyList();
        }

        List<FieldMetaCache.FieldMeta> sourceMetas = FieldMetaCache.resolveFieldMeta(query.getClass());
        return sourceMetas.stream()
                .map(meta -> new FieldMetaValue(
                        meta.getValue(query),
                        meta
                ))
                .filter(it -> Objects.nonNull(it.getValue()))
                .toList();
    }

    @Data
    @AllArgsConstructor
    public static class FieldMetaValue {

        private Object value;

        private FieldMetaCache.FieldMeta fieldMeta;

    }

}