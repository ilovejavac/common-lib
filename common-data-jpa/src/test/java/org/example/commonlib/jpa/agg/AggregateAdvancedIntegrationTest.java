package org.example.commonlib.jpa.agg;

import com.dev.lib.entity.dsl.Condition;
import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.entity.dsl.QueryType;
import com.dev.lib.jpa.entity.BaseRepository;
import com.dev.lib.jpa.entity.JpaEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import java.beans.ConstructorProperties;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AggregateAdvancedIntegrationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withUserConfiguration(AggregateAdvancedApplication.class)
            .withPropertyValues(
                    "spring.datasource.url=jdbc:h2:mem:agg_advanced_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                    "spring.datasource.driver-class-name=org.h2.Driver",
                    "spring.datasource.username=sa",
                    "spring.datasource.password=",
                    "spring.jpa.hibernate.ddl-auto=create-drop",
                    "spring.jpa.open-in-view=false",
                    "spring.application.name=aggregate-advanced-test"
            );

    @Test
    void shouldApplyHavingOrderAndPageForAggregate() {

        contextRunner.run(context -> {
            AdvUserRepo repo = context.getBean(AdvUserRepo.class);
            seedUsers(repo);

            AdvUserQuery query = new AdvUserQuery();
            query.setAvgAgeGe(20d);
            query.agg(AdvAggPageResult.class)
                    .groupBy(AdvUser::getName)
                    .field(AdvUser::getName).to(AdvAggPageResult::setName)
                    .avg(AdvUser::getAge).to(AdvAggPageResult::setAvgAge)
                    .having(AdvUserQuery::getAvgAgeGe)
                    .orderByDesc(AdvAggPageResult::getAvgAge)
                    .page(1, 1);

            List<AdvAggPageResult> rows = repo.<AdvAggPageResult>aggregate(query);

            assertThat(rows).hasSize(1);
            assertThat(rows.getFirst().getName()).isEqualTo("B");
            assertThat(rows.getFirst().getAvgAge()).isEqualTo(30.0d);
        });
    }

    @Test
    void shouldSupportSetterRecordAndConstructorResultMapping() {

        contextRunner.run(context -> {
            AdvUserRepo repo = context.getBean(AdvUserRepo.class);
            seedUsers(repo);

            AdvUserQuery setterQuery = new AdvUserQuery();
            setterQuery.agg(AdvSetterAliasResult.class)
                    .groupBy(AdvUser::getName)
                    .field(AdvUser::getName).to(AdvSetterAliasResult::setAliasName)
                    .orderByAsc(AdvSetterAliasResult::getAliasName);

            List<AdvSetterAliasResult> setterRows = repo.<AdvSetterAliasResult>aggregate(setterQuery);
            assertThat(setterRows).hasSize(3);
            assertThat(setterRows.stream().map(AdvSetterAliasResult::getPayload).sorted().toList())
                    .containsExactly("A", "B", "C");

            AdvUserQuery recordQuery = new AdvUserQuery();
            recordQuery.agg(AdvUserAvgRecord.class)
                    .groupBy(AdvUser::getName)
                    .field(AdvUser::getName).to(AdvUserAvgRecord::name)
                    .avg(AdvUser::getAge).to(AdvUserAvgRecord::avgAge)
                    .orderByAsc(AdvUserAvgRecord::name);

            List<AdvUserAvgRecord> recordRows = repo.<AdvUserAvgRecord>aggregate(recordQuery);
            assertThat(recordRows).hasSize(3);
            assertThat(recordRows.getFirst().name()).isEqualTo("A");
            assertThat(recordRows.getFirst().avgAge()).isEqualTo(20.0d);

            AdvUserQuery ctorQuery = new AdvUserQuery();
            ctorQuery.agg(AdvUserAvgCtor.class)
                    .groupBy(AdvUser::getName)
                    .field(AdvUser::getName).to(AdvUserAvgCtor::getName)
                    .avg(AdvUser::getAge).to(AdvUserAvgCtor::getAvgAge)
                    .orderByAsc(AdvUserAvgCtor::getName);

            List<AdvUserAvgCtor> ctorRows = repo.<AdvUserAvgCtor>aggregate(ctorQuery);
            assertThat(ctorRows).hasSize(3);
            assertThat(ctorRows.get(1).getName()).isEqualTo("B");
            assertThat(ctorRows.get(1).getAvgAge()).isEqualTo(30.0d);
        });
    }

    @Test
    void shouldApplyJoinStrategyForAggregateFieldPath() {

        contextRunner.run(context -> {
            AdvGoodsRepo goodsRepo = context.getBean(AdvGoodsRepo.class);
            AdvOrderRepo orderRepo = context.getBean(AdvOrderRepo.class);
            seedOrders(goodsRepo, orderRepo);

            AdvOrderQuery innerQuery = new AdvOrderQuery();
            innerQuery.agg(AdvGoodsGroupResult.class)
                    .joinInner()
                    .groupBy("goods.name")
                    .field("goods.name").to(AdvGoodsGroupResult::setGoodsName)
                    .count(AdvOrder::getId).to(AdvGoodsGroupResult::setCountValue);

            List<AdvGoodsGroupResult> innerRows = orderRepo.<AdvGoodsGroupResult>aggregate(innerQuery);
            assertThat(innerRows).hasSize(1);
            assertThat(innerRows.getFirst().getGoodsName()).isEqualTo("G1");
            assertThat(innerRows.getFirst().getCountValue()).isEqualTo(1L);

            AdvOrderQuery leftQuery = new AdvOrderQuery();
            leftQuery.agg(AdvGoodsGroupResult.class)
                    .joinLeft()
                    .groupBy("goods.name")
                    .field("goods.name").to(AdvGoodsGroupResult::setGoodsName)
                    .count(AdvOrder::getId).to(AdvGoodsGroupResult::setCountValue);

            List<AdvGoodsGroupResult> leftRows = orderRepo.<AdvGoodsGroupResult>aggregate(leftQuery);
            Map<String, Long> countByGoodsName = leftRows.stream()
                    .collect(Collectors.toMap(
                            row -> String.valueOf(row.getGoodsName()),
                            AdvGoodsGroupResult::getCountValue,
                            Long::sum
                    ));
            assertThat(leftRows).hasSize(2);
            assertThat(countByGoodsName.get("G1")).isEqualTo(1L);
            assertThat(countByGoodsName.get("null")).isEqualTo(1L);
        });
    }

    @Test
    void shouldFailFastOnIllegalAggregateSpec() {

        contextRunner.run(context -> {
            AdvUserRepo repo = context.getBean(AdvUserRepo.class);
            seedUsers(repo);

            AdvUserQuery duplicateTarget = new AdvUserQuery();
            duplicateTarget.agg(AdvAggPageResult.class)
                    .groupBy(AdvUser::getName)
                    .avg(AdvUser::getAge).to(AdvAggPageResult::getAvgAge)
                    .sum(AdvUser::getAge).to(AdvAggPageResult::getAvgAge);

            assertThatThrownBy(() -> repo.aggregate(duplicateTarget))
                    .hasRootCauseInstanceOf(IllegalArgumentException.class)
                    .hasRootCauseMessage("聚合目标字段重复映射: avgAge");

            AdvUserQuery fieldNotGrouped = new AdvUserQuery();
            fieldNotGrouped.agg(AdvAggPageResult.class)
                    .groupBy(AdvUser::getName)
                    .field(AdvUser::getTitle).to(AdvAggPageResult::getName);

            assertThatThrownBy(() -> repo.aggregate(fieldNotGrouped))
                    .hasRootCauseInstanceOf(IllegalArgumentException.class)
                    .hasRootCauseMessage("FIELD 投影字段必须出现在 groupBy 中: title");

            AdvUserQuery nonNumericSum = new AdvUserQuery();
            nonNumericSum.agg(AdvAggPageResult.class)
                    .groupBy(AdvUser::getName)
                    .sum("name").to(AdvAggPageResult::getAvgAge);

            assertThatThrownBy(() -> repo.aggregate(nonNumericSum))
                    .hasRootCauseInstanceOf(IllegalArgumentException.class)
                    .hasRootCauseMessage("聚合字段必须是数值类型: name for SUM");

            AdvUserQuery havingUnmapped = new AdvUserQuery();
            havingUnmapped.setNotMappedGe(1d);
            havingUnmapped.agg(AdvAggPageResult.class)
                    .groupBy(AdvUser::getName)
                    .field(AdvUser::getName).to(AdvAggPageResult::getName)
                    .having(AdvUserQuery::getNotMappedGe);

            assertThatThrownBy(() -> repo.aggregate(havingUnmapped))
                    .hasRootCauseInstanceOf(IllegalArgumentException.class)
                    .hasRootCauseMessage("having 字段未映射: notMapped");
        });
    }

    private void seedUsers(AdvUserRepo repo) {

        repo.deleteAll();
        repo.saveAll(List.of(
                new AdvUser("A", "t1", 10),
                new AdvUser("A", "t2", 30),
                new AdvUser("B", "t1", 20),
                new AdvUser("B", "t2", 40),
                new AdvUser("C", "t1", 40)
        ));
    }

    private void seedOrders(AdvGoodsRepo goodsRepo, AdvOrderRepo orderRepo) {

        orderRepo.deleteAll();
        goodsRepo.deleteAll();

        AdvGoods goods = goodsRepo.save(new AdvGoods("G1"));

        orderRepo.saveAll(List.of(
                new AdvOrder("o1", goods),
                new AdvOrder("o2", null)
        ));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class AggregateAdvancedApplication {
    }
}

@Entity
class AdvUser extends JpaEntity {

    private String name;

    private String title;

    private Integer age;

    AdvUser() {
    }

    AdvUser(String name, String title, Integer age) {

        this.name = name;
        this.title = title;
        this.age = age;
    }

    public String getName() {

        return name;
    }

    public String getTitle() {

        return title;
    }

    public Integer getAge() {

        return age;
    }
}

interface AdvUserRepo extends BaseRepository<AdvUser> {
}

class AdvUserQuery extends DslQuery<AdvUser> {

    @Condition(field = "avgAge", type = QueryType.GE)
    private Double avgAgeGe;

    private Double notMappedGe;

    public Double getAvgAgeGe() {

        return avgAgeGe;
    }

    public void setAvgAgeGe(Double avgAgeGe) {

        this.avgAgeGe = avgAgeGe;
    }

    public Double getNotMappedGe() {

        return notMappedGe;
    }

    public void setNotMappedGe(Double notMappedGe) {

        this.notMappedGe = notMappedGe;
    }
}

class AdvAggPageResult {

    private String name;

    private Double avgAge;

    public String getName() {

        return name;
    }

    public void setName(String name) {

        this.name = name;
    }

    public Double getAvgAge() {

        return avgAge;
    }

    public void setAvgAge(Double avgAge) {

        this.avgAge = avgAge;
    }
}

record AdvUserAvgRecord(String name, Double avgAge) {
}

class AdvUserAvgCtor {

    private final String name;

    private final Double avgAge;

    @ConstructorProperties({
            "name",
            "avgAge"
    })
    AdvUserAvgCtor(String name, Double avgAge) {

        this.name = name;
        this.avgAge = avgAge;
    }

    public String getName() {

        return name;
    }

    public Double getAvgAge() {

        return avgAge;
    }
}

class AdvSetterAliasResult {

    private String payload;

    public void setAliasName(String aliasName) {

        this.payload = aliasName;
    }

    public String getAliasName() {

        return payload;
    }

    public String getPayload() {

        return payload;
    }
}

@Entity
class AdvGoods extends JpaEntity {

    private String name;

    AdvGoods() {
    }

    AdvGoods(String name) {

        this.name = name;
    }

    public String getName() {

        return name;
    }
}

interface AdvGoodsRepo extends BaseRepository<AdvGoods> {
}

@Entity
class AdvOrder extends JpaEntity {

    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    private AdvGoods goods;

    AdvOrder() {
    }

    AdvOrder(String title, AdvGoods goods) {

        this.title = title;
        this.goods = goods;
    }

    public String getTitle() {

        return title;
    }

    public AdvGoods getGoods() {

        return goods;
    }
}

interface AdvOrderRepo extends BaseRepository<AdvOrder> {
}

class AdvOrderQuery extends DslQuery<AdvOrder> {
}

class AdvGoodsGroupResult {

    private String goodsName;

    private Long countValue;

    public String getGoodsName() {

        return goodsName;
    }

    public void setGoodsName(String goodsName) {

        this.goodsName = goodsName;
    }

    public Long getCountValue() {

        return countValue;
    }

    public void setCountValue(Long countValue) {

        this.countValue = countValue;
    }
}
