package com.dev.lib.mongo.dsl;

import com.dev.lib.entity.dsl.Condition;
import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.entity.dsl.core.QueryFieldMerger;
import com.dev.lib.mongo.MongoEntity;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Operation;
import com.querydsl.core.types.Ops;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.PathInits;
import com.querydsl.core.types.dsl.StringPath;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.querydsl.core.types.PathMetadataFactory.forVariable;
import static org.assertj.core.api.Assertions.assertThat;

class PredicateAssemblerLogicTest {

    @Test
    void shouldUseAndOnlyForFlatQuery() {

        QueryFlat query = new QueryFlat();
        query.c1 = "C1";
        query.c2Gt = 10;
        query.c3 = "C3";

        Predicate predicate = assemble(query);
        List<String> groups = toCanonicalGroups(predicate);

        assertThat(groups).containsExactly("testMongoEntity.c1&testMongoEntity.c2&testMongoEntity.c3");
    }

    @Test
    void shouldUseOnlyArrangedFieldsWhenLogicPresent() {

        QueryLogicArrange query = new QueryLogicArrange();
        query.setC1("C1");
        query.setC2Gt(10);
        query.setC3("C3");
        query.setC4("C4");
        query.where().use(QueryLogicArrange::getC1).or(QueryLogicArrange::getC4);

        Predicate predicate = assemble(query);
        List<String> groups = toCanonicalGroups(predicate);

        assertThat(groups).containsExactlyInAnyOrder("testMongoEntity.c1", "testMongoEntity.c4");
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

        Predicate predicate = assemble(query);
        List<String> groups = toCanonicalGroups(predicate);

        assertThat(groups).containsExactlyInAnyOrder("testMongoEntity.c1", "testMongoEntity.c2&testMongoEntity.c3");
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

        Predicate predicate = assemble(query);
        List<String> groups = toCanonicalGroups(predicate);

        assertThat(groups).containsExactly("testMongoEntity.c2&testMongoEntity.c3");
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

        Predicate predicate = assemble(query);
        List<String> groups = toCanonicalGroups(predicate);

        assertThat(groups).containsExactlyInAnyOrder(
                "testMongoEntity.c1",
                "testMongoEntity.c2&testMongoEntity.c3",
                "testMongoEntity.c4"
        );
    }

    @Test
    void shouldBeTolerantToBrokenArrangement() {

        QueryLogicArrange query = new QueryLogicArrange();
        query.setC1("C1");
        query.setC2Gt(10);
        query.where().use(QueryLogicArrange::getC1)
                .orBegin()
                .use(QueryLogicArrange::getC2Gt);

        Predicate predicate = assemble(query);
        List<String> groups = toCanonicalGroups(predicate);

        assertThat(groups).containsExactlyInAnyOrder("testMongoEntity.c1", "testMongoEntity.c2");
    }

    @Test
    void shouldIgnorePathConditionWhenNotReferencedByLogic() {

        QueryWithPathCondition query = new QueryWithPathCondition();
        query.setGoodsBizId("G-1");
        query.setC1("C1");
        query.where().use(QueryWithPathCondition::getC1);

        Predicate predicate = assemble(query);
        List<String> groups = toCanonicalGroups(predicate);

        assertThat(groups).containsExactly("testMongoEntity.c1");
    }

    @Test
    void shouldSupportOrBetweenSelfAndPathConditions() {

        QueryWithPathCondition query = new QueryWithPathCondition();
        query.setGoodsBizId("G-1");
        query.setC1("C1");
        query.where().use(QueryWithPathCondition::getC1)
                .or(QueryWithPathCondition::getGoodsBizId);

        Predicate predicate = assemble(query);
        List<String> groups = toCanonicalGroups(predicate);

        assertThat(groups).containsExactlyInAnyOrder("testMongoEntity.c1", "testMongoEntity.goods.bizId");
    }

    @Test
    void shouldUseInWhenCollectionFieldHasNoTypeSuffix() {

        QueryCollectionDefaultIn query = new QueryCollectionDefaultIn();
        query.setC1(List.of("A", "B"));

        Predicate predicate = assemble(query);
        assertThat(predicate).isInstanceOf(Operation.class);

        Operation<?> operation = (Operation<?>) predicate;
        assertThat(operation.getOperator()).isEqualTo(Ops.IN);
        assertThat(operation.getArg(0).toString()).isEqualTo("testMongoEntity.c1");
    }

    private static Predicate assemble(DslQuery<TestMongoEntity> query) {

        Collection<QueryFieldMerger.FieldMetaValue> merged = QueryFieldMerger.resolve(query);
        BooleanBuilder builder = PredicateAssembler.assemble(query, merged);
        return builder.getValue();
    }

    private static List<String> toCanonicalGroups(Predicate predicate) {

        assertThat(predicate).isNotNull();

        return toDisjunction((Expression<?>) predicate).stream()
                .map(group -> group.stream().sorted().collect(Collectors.joining("&")))
                .sorted()
                .toList();
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
            return List.of(new LinkedHashSet<>(Set.of(operation.getArg(0).toString())));
        }
        return List.of(new LinkedHashSet<>(Set.of(expression.toString())));
    }
}

class QueryFlat extends DslQuery<TestMongoEntity> {

    public String c1;

    public Integer c2Gt;

    public String c3;
}

class QueryLogicArrange extends DslQuery<TestMongoEntity> {

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

class TestMongoEntity extends MongoEntity {

    private String c1;

    private Integer c2;

    private String c3;

    private String c4;
}

class QueryWithPathCondition extends DslQuery<TestMongoEntity> {

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

class QueryCollectionDefaultIn extends DslQuery<TestMongoEntity> {

    private Collection<String> c1;

    public Collection<String> getC1() {

        return c1;
    }

    public void setC1(Collection<String> c1) {

        this.c1 = c1;
    }
}

class QTestMongoEntity extends EntityPathBase<TestMongoEntity> {

    private static final long serialVersionUID = 1L;

    public static final QTestMongoEntity testMongoEntity = new QTestMongoEntity("testMongoEntity");

    public final StringPath c1 = createString("c1");

    public final NumberPath<Integer> c2 = createNumber("c2", Integer.class);

    public final StringPath c3 = createString("c3");

    public final StringPath c4 = createString("c4");

    QTestMongoEntity(String variable) {

        this(TestMongoEntity.class, forVariable(variable), PathInits.DIRECT2);
    }

    QTestMongoEntity(PathMetadata metadata) {

        this(TestMongoEntity.class, metadata, PathInits.DIRECT2);
    }

    QTestMongoEntity(PathMetadata metadata, PathInits inits) {

        this(TestMongoEntity.class, metadata, inits);
    }

    QTestMongoEntity(Class<? extends TestMongoEntity> type, PathMetadata metadata, PathInits inits) {

        super(type, metadata, inits);
    }
}
