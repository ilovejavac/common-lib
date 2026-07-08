package com.dev.lib.jpa.entity.dsl;

import com.dev.lib.entity.dsl.BizRef;
import com.dev.lib.entity.dsl.Condition;
import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.entity.dsl.core.QueryFieldMerger;
import com.dev.lib.jpa.entity.JpaEntity;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Operation;
import com.querydsl.core.types.Ops;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.SubQueryExpression;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.PathInits;
import com.querydsl.core.types.dsl.StringPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.querydsl.core.types.PathMetadataFactory.forVariable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PredicateAssemblerLogicTest {

    @BeforeAll
    static void registerRelationResolver() {

        new JpaRelationResolver().init();
    }

    @Test
    void shouldUseAndOnlyForFlatQuery() {

        QueryFlat query = new QueryFlat();
        query.c1 = "C1";
        query.c2 = "C2";
        query.c3 = "C3";

        Predicate predicate = assemble(query);

        List<String> groups = toCanonicalGroups(predicate);

        assertThat(groups).containsExactly("testJpaEntity.c1&testJpaEntity.c2&testJpaEntity.c3");
    }

    @Test
    void shouldIgnoreGroupQuery() {

        QueryWithGroup query = new QueryWithGroup();
        query.group = new GroupFilter();
        query.group.c1 = "C1";

        Predicate predicate = assemble(query);
        assertThat(predicate).isNull();
    }

    @Test
    void shouldUseAndOnlyInsideSelfSubQuery() {

        QuerySelfSub query = new QuerySelfSub();
        query.subExistsSub = new SubFilter();
        query.subExistsSub.c1 = "C1";
        query.subExistsSub.c2 = "C2";

        Predicate predicate = assemble(query);
        SubQueryExpression<?> subQueryExpression = findFirstSubQueryExpression((Expression<?>) predicate);

        assertThat(subQueryExpression).isNotNull();
        Predicate where = subQueryExpression.getMetadata().getWhere();
        assertThat(where).isNotNull();

        List<String> groups = toCanonicalGroups(where);
        assertThat(groups).containsExactly("testJpaEntity_sub.c1&testJpaEntity_sub.c2");
    }

    @Test
    void shouldIgnoreGroupInsideSelfSubQuery() {

        QuerySelfSubNestedGroup query = new QuerySelfSubNestedGroup();
        query.subExistsSub = new SubFilterNested();
        query.subExistsSub.inner = new InnerGroup();
        query.subExistsSub.inner.c2 = "C2";
        query.subExistsSub.inner.c3 = "C3";

        Predicate predicate = assemble(query);
        SubQueryExpression<?> subQueryExpression = findFirstSubQueryExpression((Expression<?>) predicate);

        assertThat(subQueryExpression).isNotNull();
        Predicate where = subQueryExpression.getMetadata().getWhere();
        assertThat(where).isNull();
    }

    @Test
    void shouldUseOnlyArrangedFieldsWhenLogicPresent() {

        QueryLogicArrange query = new QueryLogicArrange();
        query.setC1("C1");
        query.setC2("C2");
        query.setC3("C3");
        query.setC4("C4");
        query.where().use(QueryLogicArrange::getC1).or(QueryLogicArrange::getC4);

        Predicate predicate = assemble(query);
        List<String> groups = toCanonicalGroups(predicate);

        assertThat(groups).containsExactlyInAnyOrder("testJpaEntity.c1", "testJpaEntity.c4");
    }

    @Test
    void shouldRespectSqlPrecedenceInLogicArrangement() {

        QueryLogicArrange query = new QueryLogicArrange();
        query.setC1("C1");
        query.setC2("C2");
        query.setC3("C3");
        query.where().use(QueryLogicArrange::getC1)
                .or(QueryLogicArrange::getC2)
                .and(QueryLogicArrange::getC3);

        Predicate predicate = assemble(query);
        List<String> groups = toCanonicalGroups(predicate);

        assertThat(groups).containsExactlyInAnyOrder("testJpaEntity.c1", "testJpaEntity.c2&testJpaEntity.c3");
    }

    @Test
    void shouldSkipNullValueInLogicArrangement() {

        QueryLogicArrange query = new QueryLogicArrange();
        query.setC1(null);
        query.setC2("C2");
        query.setC3("C3");
        query.where().use(QueryLogicArrange::getC1)
                .or(QueryLogicArrange::getC2)
                .and(QueryLogicArrange::getC3);

        Predicate predicate = assemble(query);
        List<String> groups = toCanonicalGroups(predicate);

        assertThat(groups).containsExactly("testJpaEntity.c2&testJpaEntity.c3");
    }

    @Test
    void shouldSupportNestedGroupingInLogicArrangement() {

        QueryLogicArrange query = new QueryLogicArrange();
        query.setC1("C1");
        query.setC2("C2");
        query.setC3("C3");
        query.setC4("C4");

        query.where().use(QueryLogicArrange::getC1)
                .orBegin()
                .use(QueryLogicArrange::getC2)
                .and(QueryLogicArrange::getC3)
                .or(QueryLogicArrange::getC4)
                .end();

        Predicate predicate = assemble(query);
        List<String> groups = toCanonicalGroups(predicate);

        assertThat(groups).containsExactlyInAnyOrder(
                "testJpaEntity.c1",
                "testJpaEntity.c2&testJpaEntity.c3",
                "testJpaEntity.c4"
        );
    }

    @Test
    void shouldBeTolerantToBrokenArrangement() {

        QueryLogicArrange query = new QueryLogicArrange();
        query.setC1("C1");
        query.setC2("C2");
        query.where().use(QueryLogicArrange::getC1)
                .orBegin()
                .use(QueryLogicArrange::getC2);

        Predicate predicate = assemble(query);
        List<String> groups = toCanonicalGroups(predicate);

        assertThat(groups).containsExactlyInAnyOrder("testJpaEntity.c1", "testJpaEntity.c2");
    }

    @Test
    void shouldUseJoinConditionWhenReferencedByLogic() {

        QueryJoinCondition query = new QueryJoinCondition();
        query.setGoodsBizId("G-1");
        query.setC1("C1");
        query.where().use(QueryJoinCondition::getGoodsBizId);

        Predicate predicate = assemble(query);
        List<String> groups = toCanonicalGroups(predicate);

        assertThat(groups).containsExactly("testJpaEntity.goods.bizId");
    }

    @Test
    void shouldIgnoreJoinConditionWhenNotReferencedByLogic() {

        QueryJoinCondition query = new QueryJoinCondition();
        query.setGoodsBizId("G-1");
        query.setC1("C1");
        query.where().use(QueryJoinCondition::getC1);

        Predicate predicate = assemble(query);
        List<String> groups = toCanonicalGroups(predicate);

        assertThat(groups).containsExactly("testJpaEntity.c1");
    }

    @Test
    void shouldIncludeSubQueryWhenReferencedByLogic() {

        QueryLogicWithSub query = new QueryLogicWithSub();
        query.setC1("C1");
        SubFilter sub = new SubFilter();
        sub.c1 = "S1";
        sub.c2 = "S2";
        query.setSubExistsSub(sub);
        query.where().use(QueryLogicWithSub::getSubExistsSub);

        Predicate predicate = assemble(query);
        SubQueryExpression<?> subQueryExpression = findFirstSubQueryExpression((Expression<?>) predicate);

        assertThat(subQueryExpression).isNotNull();
        Predicate where = subQueryExpression.getMetadata().getWhere();
        assertThat(where).isNotNull();

        List<String> groups = toCanonicalGroups(where);
        assertThat(groups).containsExactly("testJpaEntity_sub.c1&testJpaEntity_sub.c2");
    }

    @Test
    void shouldIgnoreSubQueryWhenNotReferencedByLogic() {

        QueryLogicWithSub query = new QueryLogicWithSub();
        query.setC1("C1");
        SubFilter sub = new SubFilter();
        sub.c1 = "S1";
        sub.c2 = "S2";
        query.setSubExistsSub(sub);
        query.where().use(QueryLogicWithSub::getC1);

        Predicate predicate = assemble(query);
        SubQueryExpression<?> subQueryExpression = findFirstSubQueryExpression((Expression<?>) predicate);

        assertThat(subQueryExpression).isNull();
        List<String> groups = toCanonicalGroups(predicate);
        assertThat(groups).containsExactly("testJpaEntity.c1");
    }

    @Test
    void shouldDropUnreferencedJoinConditionWhenArrangedFieldsAreNull() {

        QueryJoinCondition query = new QueryJoinCondition();
        query.setGoodsBizId("G-1");
        query.setC1(null);
        query.where().use(QueryJoinCondition::getC1);

        Predicate predicate = assemble(query);
        assertThat(predicate).isNull();
    }

    @Test
    void shouldSupportOrBetweenSelfAndJoinConditions() {

        QueryJoinCondition query = new QueryJoinCondition();
        query.setGoodsBizId("G-1");
        query.setC1("C1");
        query.where().use(QueryJoinCondition::getC1)
                .or(QueryJoinCondition::getGoodsBizId);

        Predicate predicate = assemble(query);
        List<String> groups = toCanonicalGroups(predicate);

        assertThat(groups).containsExactlyInAnyOrder("testJpaEntity.c1", "testJpaEntity.goods.bizId");
    }

    @Test
    void shouldResolveBizRefToRelationBizIdRegardlessOfFieldName() {

        QueryBizRefCondition query = new QueryBizRefCondition();
        query.setAnyName("G-1");

        Predicate predicate = assemble(query);
        List<String> groups = toCanonicalGroups(predicate);

        assertThat(groups).containsExactly("testJpaEntity.goods.bizId");
    }

    @Test
    void shouldResolveBizRefWithoutValueFromQueryFieldName() {

        QueryImplicitBizRefCondition query = new QueryImplicitBizRefCondition();
        query.setGoods("G-1");

        Predicate predicate = assemble(query);
        List<String> groups = toCanonicalGroups(predicate);

        assertThat(groups).containsExactly("testJpaEntity.goods.bizId");
    }

    @Test
    void shouldResolveBizRefWithoutValueAndKeepQueryTypeSuffix() {

        QueryImplicitBizRefInCondition query = new QueryImplicitBizRefInCondition();
        query.setGoodsIn(List.of("G-1", "G-2"));

        Predicate predicate = assemble(query);

        assertThat(predicate).isInstanceOf(Operation.class);
        Operation<?> operation = (Operation<?>) predicate;
        assertThat(operation.getOperator()).isEqualTo(Ops.IN);
        assertThat(operation.getArg(0).toString()).isEqualTo("testJpaEntity.goods.bizId");
    }

    @Test
    void conditionFieldShouldOverrideBizRef() {

        QueryConditionOverridesBizRef query = new QueryConditionOverridesBizRef();
        query.setAnyName("Widget");

        Predicate predicate = assemble(query);
        List<String> groups = toCanonicalGroups(predicate);

        assertThat(groups).containsExactly("testJpaEntity.goods.name");
    }

    @Test
    void blankBizRefShouldFailFastDuringMetadataResolution() {

        QueryBlankBizRef query = new QueryBlankBizRef();
        query.setAnyName("G-1");

        assertThatThrownBy(() -> QueryFieldMerger.resolve(query))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("@BizRef value 不能为空");
    }

    @Test
    void shouldResolveRelationBizIdSuffixToRelationBizId() {

        QueryBizIdSuffixCondition query = new QueryBizIdSuffixCondition();
        query.setGoodsBizId("G-1");

        Predicate predicate = assemble(query);
        List<String> groups = toCanonicalGroups(predicate);

        assertThat(groups).containsExactly("testJpaEntity.goods.bizId");
    }

    @Test
    void shouldResolveRelationBizIdInSuffixToRelationBizIdIn() {

        QueryBizIdInSuffixCondition query = new QueryBizIdInSuffixCondition();
        query.setGoodsBizIdIn(List.of("G-1", "G-2"));

        Predicate predicate = assemble(query);

        assertThat(predicate).isInstanceOf(Operation.class);
        Operation<?> operation = (Operation<?>) predicate;
        assertThat(operation.getOperator()).isEqualTo(Ops.IN);
        assertThat(operation.getArg(0).toString()).isEqualTo("testJpaEntity.goods.bizId");
    }

    @Test
    void lowercaseBizIdSuffixShouldKeepExistingFieldBehavior() {

        QueryLowercaseBizIdSuffixCondition query = new QueryLowercaseBizIdSuffixCondition();
        query.setGoodsbizid("G-1");

        Predicate predicate = assemble(query);
        List<String> groups = toCanonicalGroups(predicate);

        assertThat(groups).containsExactly("testJpaEntity.goodsbizid");
    }

    @Test
    void nonRelationBizIdSuffixShouldKeepExistingFieldBehavior() {

        QueryNonRelationBizIdSuffixCondition query = new QueryNonRelationBizIdSuffixCondition();
        query.setExternalBizId("E-1");

        Predicate predicate = assemble(query);
        List<String> groups = toCanonicalGroups(predicate);

        assertThat(groups).containsExactly("testJpaEntity.externalBizId");
    }

    @Test
    void shouldUseInWhenCollectionFieldHasNoTypeSuffix() {

        QueryCollectionDefaultIn query = new QueryCollectionDefaultIn();
        query.setC1(List.of("A", "B"));

        Predicate predicate = assemble(query);
        assertThat(predicate).isInstanceOf(Operation.class);

        Operation<?> operation = (Operation<?>) predicate;
        assertThat(operation.getOperator()).isEqualTo(Ops.IN);
        assertThat(operation.getArg(0).toString()).isEqualTo("testJpaEntity.c1");
    }

    @Test
    void shouldMergeGeAndLeIntoBetweenForDefaultAndQuery() {

        QueryRangeCondition query = new QueryRangeCondition();
        query.setC1Ge("A");
        query.setC1Le("Z");

        Predicate predicate = assemble(query);

        assertBetweenPredicate(
                predicate,
                "testJpaEntity.c1"
        );
    }

    @Test
    void shouldKeepGeAndLeSeparateWhenLogicArrangementIsPresent() {

        QueryRangeCondition query = new QueryRangeCondition();
        query.setC1Ge("A");
        query.setC1Le("Z");
        query.where()
                .use(QueryRangeCondition::getC1Ge)
                .and(QueryRangeCondition::getC1Le);

        Predicate predicate = assemble(query);

        assertThat(predicate).isInstanceOf(Operation.class);
        Operation<?> operation = (Operation<?>) predicate;
        assertThat(operation.getOperator()).isEqualTo(Ops.AND);
        assertThat(((Operation<?>) operation.getArg(0)).getOperator()).isEqualTo(Ops.GOE);
        assertThat(((Operation<?>) operation.getArg(1)).getOperator()).isEqualTo(Ops.LOE);
    }

    @Test
    void shouldSupportBetweenSuffixWithTwoBounds() {

        QueryBetweenCondition query = new QueryBetweenCondition();
        query.setC1Between(List.of("A", "Z"));

        Predicate predicate = assemble(query);

        assertBetweenPredicate(
                predicate,
                "testJpaEntity.c1"
        );
    }

    @Test
    void shouldMergeGeAndLeIntoBetweenInsideSubQueryFilter() {

        QuerySelfSubRange query = new QuerySelfSubRange();
        query.subExistsSub = new SubRangeFilter();
        query.subExistsSub.c1Ge = "A";
        query.subExistsSub.c1Le = "Z";

        Predicate predicate = assemble(query);
        SubQueryExpression<?> subQueryExpression = findFirstSubQueryExpression((Expression<?>) predicate);

        assertThat(subQueryExpression).isNotNull();
        Predicate where = subQueryExpression.getMetadata().getWhere();
        assertBetweenPredicate(
                where,
                "testJpaEntity_sub.c1"
        );
    }

    private static Predicate assemble(DslQuery<TestJpaEntity> query) {

        Collection<QueryFieldMerger.FieldMetaValue> merged = QueryFieldMerger.resolve(query);
        BooleanBuilder builder = PredicateAssembler.assemble(query, merged);
        return builder.getValue();
    }

    private static void assertBetweenPredicate(Predicate predicate, String expectedPath) {

        assertThat(predicate).isInstanceOf(Operation.class);
        Operation<?> operation = (Operation<?>) predicate;
        assertThat(operation.getOperator()).isEqualTo(Ops.BETWEEN);
        assertThat(operation.getArg(0).toString()).isEqualTo(expectedPath);
    }

    private static List<String> toCanonicalGroups(Predicate predicate) {

        assertThat(predicate).isNotNull();

        return toDisjunction((Expression<?>) predicate).stream()
                .map(group -> group.stream().sorted().collect(Collectors.joining("&")))
                .sorted()
                .toList();
    }

    @SuppressWarnings("unchecked")
    private static SubQueryExpression<?> findFirstSubQueryExpression(Expression<?> expression) {

        if (expression == null) {
            return null;
        }
        if (expression instanceof SubQueryExpression<?> subQueryExpression) {
            return subQueryExpression;
        }
        if (expression instanceof Operation<?> operation) {
            for (Expression<?> arg : (List<Expression<?>>) operation.getArgs()) {
                SubQueryExpression<?> found = findFirstSubQueryExpression(arg);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static List<Set<String>> toDisjunction(Expression<?> expression) {

        if (expression instanceof Operation<?> operation) {
            if (operation.getOperator() == Ops.OR) {
                List<Set<String>> result = new ArrayList<>();
                for (Expression<?> arg : (List<Expression<?>>) operation.getArgs()) {
                    result.addAll(toDisjunction(arg));
                }
                return result;
            }
            if (operation.getOperator() == Ops.AND) {
                List<Set<String>> result = new ArrayList<>();
                result.add(new LinkedHashSet<>());
                for (Expression<?> arg : (List<Expression<?>>) operation.getArgs()) {
                    List<Set<String>> partial = toDisjunction(arg);
                    List<Set<String>> combined = new ArrayList<>();
                    for (Set<String> left : result) {
                        for (Set<String> right : partial) {
                            Set<String> merged = new LinkedHashSet<>(left);
                            merged.addAll(right);
                            combined.add(merged);
                        }
                    }
                    result = combined;
                }
                return result;
            }
            if (operation.getOperator() == Ops.EQ) {
                return List.of(new LinkedHashSet<>(Set.of(operation.getArg(0).toString())));
            }
            return List.of(new LinkedHashSet<>(Set.of(operation.getArg(0).toString())));
        }
        return List.of(new LinkedHashSet<>(Set.of(expression.toString())));
    }
}

class QueryFlat extends DslQuery<TestJpaEntity> {

    public String c1;

    public String c2;

    public String c3;
}

class QueryWithGroup extends DslQuery<TestJpaEntity> {

    public GroupFilter group;
}

class GroupFilter {

    public String c1;
}

class QuerySelfSub extends DslQuery<TestJpaEntity> {

    public SubFilter subExistsSub;
}

class SubFilter {

    public String c1;

    public String c2;
}

class QuerySelfSubNestedGroup extends DslQuery<TestJpaEntity> {

    public SubFilterNested subExistsSub;
}

class QuerySelfSubRange extends DslQuery<TestJpaEntity> {

    public SubRangeFilter subExistsSub;
}

class SubFilterNested {

    public InnerGroup inner;
}

class SubRangeFilter {

    public String c1Ge;

    public String c1Le;
}

class InnerGroup {

    public String c2;

    public String c3;
}

class QueryLogicArrange extends DslQuery<TestJpaEntity> {

    private String c1;

    private String c2;

    private String c3;

    private String c4;

    public String getC1() {

        return c1;
    }

    public void setC1(String c1) {

        this.c1 = c1;
    }

    public String getC2() {

        return c2;
    }

    public void setC2(String c2) {

        this.c2 = c2;
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

class QueryJoinCondition extends DslQuery<TestJpaEntity> {

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

class QueryBizRefCondition extends DslQuery<TestJpaEntity> {

    @BizRef("goods")
    private String anyName;

    public String getAnyName() {

        return anyName;
    }

    public void setAnyName(String anyName) {

        this.anyName = anyName;
    }
}

class QueryImplicitBizRefCondition extends DslQuery<TestJpaEntity> {

    @BizRef
    private String goods;

    public String getGoods() {

        return goods;
    }

    public void setGoods(String goods) {

        this.goods = goods;
    }
}

class QueryImplicitBizRefInCondition extends DslQuery<TestJpaEntity> {

    @BizRef
    private Collection<String> goodsIn;

    public Collection<String> getGoodsIn() {

        return goodsIn;
    }

    public void setGoodsIn(Collection<String> goodsIn) {

        this.goodsIn = goodsIn;
    }
}

class QueryConditionOverridesBizRef extends DslQuery<TestJpaEntity> {

    @Condition(field = "goods.name")
    @BizRef("goods")
    private String anyName;

    public String getAnyName() {

        return anyName;
    }

    public void setAnyName(String anyName) {

        this.anyName = anyName;
    }
}

class QueryBlankBizRef extends DslQuery<TestJpaEntity> {

    @BizRef("")
    private String anyName;

    public String getAnyName() {

        return anyName;
    }

    public void setAnyName(String anyName) {

        this.anyName = anyName;
    }
}

class QueryBizIdSuffixCondition extends DslQuery<TestJpaEntity> {

    private String goodsBizId;

    public String getGoodsBizId() {

        return goodsBizId;
    }

    public void setGoodsBizId(String goodsBizId) {

        this.goodsBizId = goodsBizId;
    }
}

class QueryBizIdInSuffixCondition extends DslQuery<TestJpaEntity> {

    private Collection<String> goodsBizIdIn;

    public Collection<String> getGoodsBizIdIn() {

        return goodsBizIdIn;
    }

    public void setGoodsBizIdIn(Collection<String> goodsBizIdIn) {

        this.goodsBizIdIn = goodsBizIdIn;
    }
}

class QueryLowercaseBizIdSuffixCondition extends DslQuery<TestJpaEntity> {

    private String goodsbizid;

    public String getGoodsbizid() {

        return goodsbizid;
    }

    public void setGoodsbizid(String goodsbizid) {

        this.goodsbizid = goodsbizid;
    }
}

class QueryNonRelationBizIdSuffixCondition extends DslQuery<TestJpaEntity> {

    private String externalBizId;

    public String getExternalBizId() {

        return externalBizId;
    }

    public void setExternalBizId(String externalBizId) {

        this.externalBizId = externalBizId;
    }
}

class QueryLogicWithSub extends DslQuery<TestJpaEntity> {

    private String c1;

    private SubFilter subExistsSub;

    public String getC1() {

        return c1;
    }

    public void setC1(String c1) {

        this.c1 = c1;
    }

    public SubFilter getSubExistsSub() {

        return subExistsSub;
    }

    public void setSubExistsSub(SubFilter subExistsSub) {

        this.subExistsSub = subExistsSub;
    }
}

class QueryCollectionDefaultIn extends DslQuery<TestJpaEntity> {

    private Collection<String> c1;

    public Collection<String> getC1() {

        return c1;
    }

    public void setC1(Collection<String> c1) {

        this.c1 = c1;
    }
}

class QueryRangeCondition extends DslQuery<TestJpaEntity> {

    private String c1Ge;

    private String c1Le;

    public String getC1Ge() {

        return c1Ge;
    }

    public void setC1Ge(String c1Ge) {

        this.c1Ge = c1Ge;
    }

    public String getC1Le() {

        return c1Le;
    }

    public void setC1Le(String c1Le) {

        this.c1Le = c1Le;
    }
}

class QueryBetweenCondition extends DslQuery<TestJpaEntity> {

    private List<String> c1Between;

    public List<String> getC1Between() {

        return c1Between;
    }

    public void setC1Between(List<String> c1Between) {

        this.c1Between = c1Between;
    }
}

class TestJpaEntity extends JpaEntity {

    private String c1;

    private String c2;

    private String c3;

    private String c4;

    @jakarta.persistence.ManyToOne
    private TestGoodsEntity goods;
}

class TestGoodsEntity extends JpaEntity {

    private String name;
}

class QTestJpaEntity extends EntityPathBase<TestJpaEntity> {

    private static final long serialVersionUID = 1L;

    public static final QTestJpaEntity testJpaEntity = new QTestJpaEntity("testJpaEntity");

    public final StringPath c1 = createString("c1");

    public final StringPath c2 = createString("c2");

    public final StringPath c3 = createString("c3");

    public final StringPath c4 = createString("c4");

    public final QTestGoodsEntity goods;

    QTestJpaEntity(String variable) {

        this(TestJpaEntity.class, forVariable(variable), PathInits.DIRECT2);
    }

    QTestJpaEntity(PathMetadata metadata) {

        this(TestJpaEntity.class, metadata, PathInits.DIRECT2);
    }

    QTestJpaEntity(PathMetadata metadata, PathInits inits) {

        this(TestJpaEntity.class, metadata, inits);
    }

    QTestJpaEntity(Class<? extends TestJpaEntity> type, PathMetadata metadata, PathInits inits) {

        super(type, metadata, inits);
        this.goods = inits.isInitialized("goods") ? new QTestGoodsEntity(forProperty("goods")) : null;
    }
}

class QTestGoodsEntity extends EntityPathBase<TestGoodsEntity> {

    private static final long serialVersionUID = 1L;

    public final StringPath bizId = createString("bizId");

    public final StringPath name = createString("name");

    QTestGoodsEntity(String variable) {

        this(TestGoodsEntity.class, forVariable(variable), PathInits.DIRECT2);
    }

    QTestGoodsEntity(PathMetadata metadata) {

        this(TestGoodsEntity.class, metadata, PathInits.DIRECT2);
    }

    QTestGoodsEntity(Class<? extends TestGoodsEntity> type, PathMetadata metadata, PathInits inits) {

        super(type, metadata, inits);
    }
}
