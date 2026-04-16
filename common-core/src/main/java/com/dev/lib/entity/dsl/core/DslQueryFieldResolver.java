package com.dev.lib.entity.dsl.core;

import com.dev.lib.entity.dsl.DslQuery;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DslQueryFieldResolver {

    private DslQueryFieldResolver() {

    }

    public enum OverridePolicy {
        SELF_OVERRIDE_EXTERNAL,
        EXTERNAL_OVERRIDE_SELF
    }

    public static Collection<QueryFieldMerger.FieldMetaValue> resolveMerged(
            DslQuery<?> query,
            OverridePolicy policy
    ) {

        List<QueryFieldMerger.FieldMetaValue> self = QueryFieldMerger.resolve(query);
        List<QueryFieldMerger.FieldMetaValue> external = query.getExternalFields();

        if (external.isEmpty()) {
            return self;
        }

        Map<String, QueryFieldMerger.FieldMetaValue> merged = new HashMap<>(self.size() + external.size());
        if (policy == OverridePolicy.SELF_OVERRIDE_EXTERNAL) {
            for (QueryFieldMerger.FieldMetaValue item : external) {
                merged.put(mergeKey(item), item);
            }
            for (QueryFieldMerger.FieldMetaValue item : self) {
                merged.put(mergeKey(item), item);
            }
        } else {
            for (QueryFieldMerger.FieldMetaValue item : self) {
                merged.put(mergeKey(item), item);
            }
            for (QueryFieldMerger.FieldMetaValue item : external) {
                merged.put(mergeKey(item), item);
            }
        }

        return merged.values();
    }

    private static String mergeKey(QueryFieldMerger.FieldMetaValue value) {

        return value.getFieldMeta().targetField() + "-" + value.getFieldMeta().queryType();
    }
}
