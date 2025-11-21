package com.dev.lib.entity.dsl.core;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QueryFieldMerger {
    private QueryFieldMerger() {
    }

    public static List<FieldMetaValue> resolve(Object query) {
        if (query == null) {
            return Collections.emptyList();
        }

        List<FieldMetaValue> externalFields = new ArrayList<>();
        List<FieldMetaCache.FieldMeta> sourceMetas = FieldMetaCache.resolveFieldMeta(query.getClass());

        for (FieldMetaCache.FieldMeta meta : sourceMetas) {
            Object value = meta.getValue(query);
            if (value == null) continue;

            externalFields.add(new FieldMetaValue(value, meta));
        }

        return externalFields;
    }

    @Data
    @AllArgsConstructor
    public static class FieldMetaValue {
        private Object value;
        private FieldMetaCache.FieldMeta fieldMeta;
    }
}