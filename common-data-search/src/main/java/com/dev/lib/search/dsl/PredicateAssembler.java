package com.dev.lib.search.dsl;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.entity.dsl.QueryType;
import com.dev.lib.entity.dsl.core.FieldMetaCache.FieldMeta;
import com.dev.lib.entity.dsl.core.QueryFieldMerger;
import com.dev.lib.entity.dsl.group.LogicalOperator;
import com.dev.lib.search.SearchEntity;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch._types.query_dsl.Query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Slf4j
public final class PredicateAssembler {

    private PredicateAssembler() {

    }

    public static <E extends SearchEntity> Query assemble(
            DslQuery<E> query,
            Collection<QueryFieldMerger.FieldMetaValue> fields,
            Query... extraQueries
    ) {

        List<QueryItem> items = new ArrayList<>();

        if (fields != null && !fields.isEmpty()) {
            items.addAll(collectQueries(fields));
        }

        for (Query extra : extraQueries) {
            if (extra != null) {
                items.add(new QueryItem(extra, LogicalOperator.AND));
            }
        }

        if (items.isEmpty()) {
            return Query.of(q -> q.matchAll(m -> m));
        }

        String minMatch = (query != null && query.getMinimumShouldMatch() != null)
                          ? query.getMinimumShouldMatch()
                          : "1";
        return buildWithPrecedence(items, minMatch);
    }

    private static List<QueryItem> collectQueries(Collection<QueryFieldMerger.FieldMetaValue> fields) {

        List<QueryItem> items = new ArrayList<>();

        for (QueryFieldMerger.FieldMetaValue fv : fields) {
            FieldMeta fm    = fv.getFieldMeta();
            Object    value = fv.getValue();
            if (value == null) continue;

            switch (fm.metaType()) {
                case CONDITION -> {
                    Query q = ExpressionBuilder.build(
                            fm.targetField(),
                            Optional.ofNullable(fm.queryType()).orElse(QueryType.EQ),
                            value
                    );
                    if (q != null) {
                        items.add(new QueryItem(q, fm.operator()));
                    }
                }

                case GROUP -> {
                    Query groupQuery = buildGroupQuery(fm, value);
                    if (groupQuery != null) {
                        items.add(new QueryItem(groupQuery, fm.operator()));
                    }
                }

                case SUB_QUERY -> {
                    Query nestedQuery = buildNestedQuery(fm, value);
                    if (nestedQuery != null) {
                        items.add(new QueryItem(nestedQuery, fm.operator()));
                    }
                }
            }
        }

        return items;
    }

    private static Query buildGroupQuery(FieldMeta groupMeta, Object groupValue) {

        List<FieldMeta> nestedMetas = groupMeta.nestedMetas();
        if (nestedMetas == null || nestedMetas.isEmpty()) return null;

        List<QueryFieldMerger.FieldMetaValue> nestedFields = new ArrayList<>();
        for (FieldMeta nested : nestedMetas) {
            Object nestedValue = nested.getValue(groupValue);
            if (nestedValue != null) {
                nestedFields.add(new QueryFieldMerger.FieldMetaValue(nestedValue, nested));
            }
        }

        if (nestedFields.isEmpty()) return null;

        List<QueryItem> nestedItems = collectQueries(nestedFields);
        return buildWithPrecedence(nestedItems, "1");
    }

    private static Query buildNestedQuery(FieldMeta fm, Object filterValue) {

        String path = resolveNestedPath(fm);
        if (path == null) {
            log.warn("OpenSearch 嵌套查询无法推断路径，已忽略: {}", fm.field().getName());
            return null;
        }

        List<FieldMeta> filterMetas = fm.filterMetas();
        if (filterMetas == null || filterMetas.isEmpty()) return null;

        List<Query> conditions = new ArrayList<>();
        for (FieldMeta nested : filterMetas) {
            if (!nested.isCondition()) continue;

            Object nestedValue = nested.getValue(filterValue);
            if (nestedValue == null) continue;

            String fullPath = path + "." + nested.targetField();
            Query q = ExpressionBuilder.build(
                    fullPath,
                    Optional.ofNullable(nested.queryType()).orElse(QueryType.EQ),
                    nestedValue
            );
            if (q != null) {
                conditions.add(q);
            }
        }

        if (conditions.isEmpty()) return null;

        Query innerQuery = Query.of(q -> q.bool(b -> b.must(conditions)));

        QueryType queryType = fm.queryType();
        if (queryType == QueryType.NOT_EXISTS) {
            return Query.of(q -> q.bool(b -> b.mustNot(
                    Query.of(nq -> nq.nested(n -> n.path(path).query(innerQuery)))
            )));
        }

        return Query.of(q -> q.nested(n -> n.path(path).query(innerQuery)));
    }

    private static String resolveNestedPath(FieldMeta fm) {

        if (fm.relationInfo() != null) {
            log.warn("OpenSearch 不支持关联子查询，已忽略: {}", fm.field().getName());
            return null;
        }

        if (fm.parentField() != null && !fm.parentField().isEmpty()) {
            return fm.parentField();
        }

        String fieldName = fm.field().getName();
        if (fieldName.endsWith("ExistsSub")) {
            return fieldName.substring(0, fieldName.length() - 9);
        }
        if (fieldName.endsWith("NotExistsSub")) {
            return fieldName.substring(0, fieldName.length() - 12);
        }
        if (fieldName.endsWith("Sub")) {
            return fieldName.substring(0, fieldName.length() - 3);
        }

        return null;
    }

    private static Query buildWithPrecedence(List<QueryItem> items, String minimumShouldMatch) {

        if (items.isEmpty()) return null;
        if (items.size() == 1) return items.get(0).query;

        List<List<QueryItem>> andGroups    = new ArrayList<>();
        List<QueryItem>       currentGroup = new ArrayList<>();

        for (QueryItem item : items) {
            currentGroup.add(item);
            if (item.operator == LogicalOperator.OR) {
                andGroups.add(new ArrayList<>(currentGroup));
                currentGroup.clear();
            }
        }
        if (!currentGroup.isEmpty()) {
            andGroups.add(currentGroup);
        }

        List<Query> groupQueries = new ArrayList<>();
        for (List<QueryItem> group : andGroups) {
            if (group.isEmpty()) continue;
            if (group.size() == 1) {
                groupQueries.add(group.get(0).query);
            } else {
                List<Query> mustQueries = group.stream().map(i -> i.query).toList();
                groupQueries.add(Query.of(q -> q.bool(b -> b.must(mustQueries))));
            }
        }

        if (groupQueries.isEmpty()) return null;
        if (groupQueries.size() == 1) return groupQueries.get(0);

        return Query.of(q -> q.bool(b -> b.should(groupQueries).minimumShouldMatch(minimumShouldMatch)));
    }

    private record QueryItem(Query query, LogicalOperator operator) {
    }

}
