package com.dev.lib.search.dsl;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.entity.dsl.QueryWhere;
import com.dev.lib.entity.dsl.QueryType;
import com.dev.lib.entity.dsl.core.FieldMetaCache.FieldMeta;
import com.dev.lib.entity.dsl.core.LogicComposer;
import com.dev.lib.entity.dsl.core.QueryFieldMerger;
import com.dev.lib.search.SearchEntity;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch._types.query_dsl.Query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

        List<Query> extras = new ArrayList<>();

        if (fields != null && !fields.isEmpty()) {
            Map<String, Query> queryByField = collectQueries(fields);
            if (query != null && query.where().hasLogic()) {
                Query arranged = buildByLogic(query.where().logicTokens(), queryByField, query.getMinimumShouldMatch());
                if (arranged != null) {
                    extras.add(arranged);
                }
            } else {
                extras.addAll(queryByField.values());
            }
        }

        for (Query extra : extraQueries) {
            if (extra != null) {
                extras.add(extra);
            }
        }

        if (extras.isEmpty()) {
            return Query.of(q -> q.matchAll(m -> m));
        }
        if (extras.size() == 1) {
            return extras.getFirst();
        }
        return Query.of(q -> q.bool(b -> b.must(extras)));
    }

    private static Map<String, Query> collectQueries(Collection<QueryFieldMerger.FieldMetaValue> fields) {

        Map<String, Query> queries = new LinkedHashMap<>();

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
                        queries.put(fm.field().getName(), q);
                    }
                }

                case GROUP -> {
                    // GROUP 元数据不参与构建，静默忽略
                }

                case SUB_QUERY -> {
                    Query nestedQuery = buildNestedQuery(fm, value);
                    if (nestedQuery != null) {
                        queries.put(fm.field().getName(), nestedQuery);
                    }
                }
            }
        }

        return queries;
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

    private static Query buildByLogic(List<QueryWhere.LogicToken> logicTokens, Map<String, Query> queryByField, String minimumShouldMatch) {

        return LogicComposer.compose(logicTokens, queryByField::get, new LogicComposer.Combiner<>() {
            @Override
            public Query and(Query left, Query right) {

                return Query.of(q -> q.bool(b -> b.must(left, right)));
            }

            @Override
            public Query or(Query left, Query right) {

                String minShouldMatch = minimumShouldMatch == null ? "1" : minimumShouldMatch;
                return Query.of(q -> q.bool(b -> b.should(left, right).minimumShouldMatch(minShouldMatch)));
            }
        });
    }

}
