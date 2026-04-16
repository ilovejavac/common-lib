package com.dev.lib.entity.dsl;

import com.dev.lib.entity.CoreEntity;
import com.dev.lib.entity.dsl.agg.AggType;
import com.dev.lib.entity.dsl.agg.AggJoinStrategy;
import com.dev.lib.entity.dsl.agg.AggregateSpec;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DslQueryAggregateSpecTest {

    @Test
    void shouldBuildWhereAndAggTogether() {

        UserQuery query = new UserQuery();
        query.setName("n1");
        query.setTitle("t1");
        query.setAvgAgeGe(10d);

        query.where()
                .use(UserQuery::getName)
                .or(UserQuery::getTitle);

        query.agg(UserAgg.class)
                .joinLeft()
                .groupBy(UserEntity::getName)
                .avg(UserEntity::getAge).to(UserAgg::getAvgAge)
                .sum(UserEntity::getAge).to(UserAgg::setSumAge)
                .count(UserEntity::getAge).to(UserAgg::getCountAge)
                .countDistinct(UserEntity::getAge).to(UserAgg::getDistinctAgeCount)
                .field(UserEntity::getName).to(UserAgg::getName)
                .having(UserQuery::getAvgAgeGe)
                .orderByDesc(UserAgg::getAvgAge)
                .page(5, 20);

        assertThat(query.where().hasLogic()).isTrue();
        assertThat(query.where().logicTokens()).hasSize(3);
        assertThat(query.hasAgg()).isTrue();

        AggregateSpec<UserEntity, UserAgg> spec = query.aggregateSpec();
        assertThat(spec.getTargetClass()).isEqualTo(UserAgg.class);
        assertThat(spec.getGroupByFields()).containsExactly("name");
        assertThat(spec.getItems()).hasSize(5);
        assertThat(spec.getJoinStrategy()).isEqualTo(AggJoinStrategy.LEFT);
        assertThat(spec.getHavings()).hasSize(1);
        assertThat(spec.getHavings().getFirst().targetField()).isEqualTo("avgAge");
        assertThat(spec.getHavings().getFirst().queryType()).isEqualTo(QueryType.GE);
        assertThat(spec.getHavings().getFirst().value()).isEqualTo(10d);
        assertThat(spec.getOrders()).hasSize(1);
        assertThat(spec.getOrders().getFirst().targetField()).isEqualTo("avgAge");
        assertThat(spec.getOrders().getFirst().asc()).isFalse();
        assertThat(spec.getOffset()).isEqualTo(5);
        assertThat(spec.getLimit()).isEqualTo(20);
        assertThat(spec.getItems().get(0).type()).isEqualTo(AggType.AVG);
        assertThat(spec.getItems().get(0).sourceField()).isEqualTo("age");
        assertThat(spec.getItems().get(0).targetField()).isEqualTo("avgAge");
        assertThat(spec.getItems().get(1).type()).isEqualTo(AggType.SUM);
        assertThat(spec.getItems().get(1).sourceField()).isEqualTo("age");
        assertThat(spec.getItems().get(1).targetField()).isEqualTo("sumAge");
        assertThat(spec.getItems().get(2).type()).isEqualTo(AggType.COUNT);
        assertThat(spec.getItems().get(2).sourceField()).isEqualTo("age");
        assertThat(spec.getItems().get(2).targetField()).isEqualTo("countAge");
        assertThat(spec.getItems().get(3).type()).isEqualTo(AggType.COUNT_DISTINCT);
        assertThat(spec.getItems().get(3).sourceField()).isEqualTo("age");
        assertThat(spec.getItems().get(3).targetField()).isEqualTo("distinctAgeCount");
        assertThat(spec.getItems().get(4).type()).isEqualTo(AggType.FIELD);
        assertThat(spec.getItems().get(4).sourceField()).isEqualTo("name");
        assertThat(spec.getItems().get(4).targetField()).isEqualTo("name");
    }

    static class UserQuery extends DslQuery<UserEntity> {

        private String name;

        private String title;

        private Double avgAgeGe;

        public String getName() {

            return name;
        }

        public void setName(String name) {

            this.name = name;
        }

        public String getTitle() {

            return title;
        }

        public void setTitle(String title) {

            this.title = title;
        }

        public Double getAvgAgeGe() {

            return avgAgeGe;
        }

        public void setAvgAgeGe(Double avgAgeGe) {

            this.avgAgeGe = avgAgeGe;
        }
    }

    static class UserEntity extends CoreEntity {

        private String name;

        private Integer age;

        public String getName() {

            return name;
        }

        public Integer getAge() {

            return age;
        }
    }

    static class UserAgg {

        private String name;

        private Double avgAge;

        private Double sumAge;

        private Long countAge;

        private Long distinctAgeCount;

        public String getName() {

            return name;
        }

        public Double getAvgAge() {

            return avgAge;
        }

        public Double getSumAge() {

            return sumAge;
        }

        public void setSumAge(Double sumAge) {

            this.sumAge = sumAge;
        }

        public Long getCountAge() {

            return countAge;
        }

        public Long getDistinctAgeCount() {

            return distinctAgeCount;
        }
    }
}
