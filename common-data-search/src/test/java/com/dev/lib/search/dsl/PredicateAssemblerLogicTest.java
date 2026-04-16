package com.dev.lib.search.dsl;

import com.dev.lib.entity.dsl.Condition;
import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.entity.dsl.core.QueryFieldMerger;
import com.dev.lib.search.SearchEntity;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class PredicateAssemblerLogicTest {

    @Test
    void shouldUseAndOnlyForFlatQuery() {

        QueryFlat query = new QueryFlat();
        query.c1 = "C1";
        query.c2Gt = 10;
        query.c3 = "C3";

        Query assembled = assemble(query);
        List<String> groups = toCanonicalGroups(assembled);

        assertThat(groups).containsExactly("c1&c2&c3");
    }

    @Test
    void shouldUseOnlyArrangedFieldsWhenLogicPresent() {

        QueryLogicArrange query = new QueryLogicArrange();
        query.setC1("C1");
        query.setC2Gt(10);
        query.setC3("C3");
        query.setC4("C4");
        query.where().use(QueryLogicArrange::getC1).or(QueryLogicArrange::getC4);

        Query assembled = assemble(query);
        List<String> groups = toCanonicalGroups(assembled);

        assertThat(groups).containsExactlyInAnyOrder("c1", "c4");
    }

    @Test
    void shouldRespectSqlPrecedenceInLogicArrangement() {

        QueryLogicArrange query = new QueryLogicArrange();
        query.setC1("C1");
        query.setC2Gt(10);
        query.setC3("C3");
        query.where().use(QueryLogicArrange::getC1)
                .or(QueryLogicArrange::getC2Gt)
                .and(QueryLogicArrange::getC3);

        Query assembled = assemble(query);
        List<String> groups = toCanonicalGroups(assembled);

        assertThat(groups).containsExactlyInAnyOrder("c1", "c2&c3");
    }

    @Test
    void shouldSkipNullValueInLogicArrangement() {

        QueryLogicArrange query = new QueryLogicArrange();
        query.setC1(null);
        query.setC2Gt(10);
        query.setC3("C3");
        query.where().use(QueryLogicArrange::getC1)
                .or(QueryLogicArrange::getC2Gt)
                .and(QueryLogicArrange::getC3);

        Query assembled = assemble(query);
        List<String> groups = toCanonicalGroups(assembled);

        assertThat(groups).containsExactly("c2&c3");
    }

    @Test
    void shouldSupportNestedGroupingInLogicArrangement() {

        QueryLogicArrange query = new QueryLogicArrange();
        query.setC1("C1");
        query.setC2Gt(10);
        query.setC3("C3");
        query.setC4("C4");

        query.where().use(QueryLogicArrange::getC1)
                .orBegin()
                .use(QueryLogicArrange::getC2Gt)
                .and(QueryLogicArrange::getC3)
                .or(QueryLogicArrange::getC4)
                .end();

        Query assembled = assemble(query);
        List<String> groups = toCanonicalGroups(assembled);

        assertThat(groups).containsExactlyInAnyOrder("c1", "c2&c3", "c4");
    }

    @Test
    void shouldBeTolerantToBrokenArrangement() {

        QueryLogicArrange query = new QueryLogicArrange();
        query.setC1("C1");
        query.setC2Gt(10);
        query.where().use(QueryLogicArrange::getC1)
                .orBegin()
                .use(QueryLogicArrange::getC2Gt);

        Query assembled = assemble(query);
        List<String> groups = toCanonicalGroups(assembled);

        assertThat(groups).containsExactlyInAnyOrder("c1", "c2");
    }

    @Test
    void shouldIgnorePathConditionWhenNotReferencedByLogic() {

        QueryWithPathCondition query = new QueryWithPathCondition();
        query.setGoodsBizId("G-1");
        query.setC1("C1");
        query.where().use(QueryWithPathCondition::getC1);

        Query assembled = assemble(query);
        List<String> groups = toCanonicalGroups(assembled);

        assertThat(groups).containsExactly("c1");
    }

    @Test
    void shouldSupportOrBetweenSelfAndPathConditions() {

        QueryWithPathCondition query = new QueryWithPathCondition();
        query.setGoodsBizId("G-1");
        query.setC1("C1");
        query.where().use(QueryWithPathCondition::getC1)
                .or(QueryWithPathCondition::getGoodsBizId);

        Query assembled = assemble(query);
        List<String> groups = toCanonicalGroups(assembled);

        assertThat(groups).containsExactlyInAnyOrder("c1", "goods.bizId");
    }

    private static Query assemble(DslQuery<TestSearchEntity> query) {

        Collection<QueryFieldMerger.FieldMetaValue> merged = QueryFieldMerger.resolve(query);
        return PredicateAssembler.assemble(query, merged);
    }

    private static List<String> toCanonicalGroups(Query query) {

        return toDisjunction(query).stream()
                .map(group -> group.stream().sorted().collect(Collectors.joining("&")))
                .filter(it -> !it.isBlank())
                .sorted()
                .toList();
    }

    private static List<Set<String>> toDisjunction(Query query) {

        if (query == null || query.isMatchAll()) {
            return List.of(new LinkedHashSet<>());
        }
        if (query.isTerm()) {
            return List.of(new LinkedHashSet<>(Set.of(normalizeField(query.term().field()))));
        }
        if (query.isRange()) {
            return List.of(new LinkedHashSet<>(Set.of(normalizeField(query.range().field()))));
        }
        if (query.isMatch()) {
            return List.of(new LinkedHashSet<>(Set.of(normalizeField(query.match().field()))));
        }
        if (query.isPrefix()) {
            return List.of(new LinkedHashSet<>(Set.of(normalizeField(query.prefix().field()))));
        }
        if (query.isWildcard()) {
            return List.of(new LinkedHashSet<>(Set.of(normalizeField(query.wildcard().field()))));
        }
        if (query.isExists()) {
            return List.of(new LinkedHashSet<>(Set.of(normalizeField(query.exists().field()))));
        }
        if (query.isTerms()) {
            return List.of(new LinkedHashSet<>(Set.of(normalizeField(query.terms().field()))));
        }
        if (query.isNested()) {
            return toDisjunction(query.nested().query());
        }
        if (query.isBool()) {
            return toDisjunctionFromBool(query.bool());
        }
        return List.of(new LinkedHashSet<>());
    }

    private static List<Set<String>> toDisjunctionFromBool(BoolQuery boolQuery) {

        List<Set<String>> result = new ArrayList<>();
        result.add(new LinkedHashSet<>());

        for (Query must : boolQuery.must()) {
            result = andCombine(result, toDisjunction(must));
        }
        for (Query filter : boolQuery.filter()) {
            result = andCombine(result, toDisjunction(filter));
        }

        if (!boolQuery.should().isEmpty()) {
            List<Set<String>> shouldGroups = new ArrayList<>();
            for (Query should : boolQuery.should()) {
                shouldGroups.addAll(toDisjunction(should));
            }
            boolean mustShould = boolQuery.must().isEmpty()
                    || (boolQuery.minimumShouldMatch() != null && !"0".equals(boolQuery.minimumShouldMatch()));
            if (mustShould) {
                result = andCombine(result, shouldGroups);
            }
        }
        return result;
    }

    private static List<Set<String>> andCombine(List<Set<String>> left, List<Set<String>> right) {

        List<Set<String>> combined = new ArrayList<>();
        for (Set<String> l : left) {
            for (Set<String> r : right) {
                Set<String> merged = new LinkedHashSet<>(l);
                merged.addAll(r);
                combined.add(merged);
            }
        }
        return combined;
    }

    private static String normalizeField(String field) {

        if (field == null) {
            return "";
        }
        return field.endsWith(".keyword") ? field.substring(0, field.length() - 8) : field;
    }
}

class QueryFlat extends DslQuery<TestSearchEntity> {

    public String c1;

    public Integer c2Gt;

    public String c3;
}

class QueryLogicArrange extends DslQuery<TestSearchEntity> {

    private String c1;

    private Integer c2Gt;

    private String c3;

    private String c4;

    public String getC1() {

        return c1;
    }

    public void setC1(String c1) {

        this.c1 = c1;
    }

    public Integer getC2Gt() {

        return c2Gt;
    }

    public void setC2Gt(Integer c2Gt) {

        this.c2Gt = c2Gt;
    }

    public String getC3() {

        return c3;
    }

    public void setC3(String c3) {

        this.c3 = c3;
    }

    public String getC4() {

        return c4;
    }

    public void setC4(String c4) {

        this.c4 = c4;
    }
}

class TestSearchEntity extends SearchEntity {

    private String c1;

    private Integer c2;

    private String c3;

    private String c4;
}

class QueryWithPathCondition extends DslQuery<TestSearchEntity> {

    @Condition(field = "goods.bizId")
    private String goodsBizId;

    private String c1;

    public String getGoodsBizId() {

        return goodsBizId;
    }

    public void setGoodsBizId(String goodsBizId) {

        this.goodsBizId = goodsBizId;
    }

    public String getC1() {

        return c1;
    }

    public void setC1(String c1) {

        this.c1 = c1;
    }
}
