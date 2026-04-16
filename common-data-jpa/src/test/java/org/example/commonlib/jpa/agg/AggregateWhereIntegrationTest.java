package org.example.commonlib.jpa.agg;

import com.dev.lib.entity.dsl.Condition;
import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.entity.dsl.QueryType;
import com.dev.lib.jpa.entity.BaseRepository;
import com.dev.lib.jpa.entity.JpaEntity;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.Test;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AggregateWhereIntegrationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withUserConfiguration(AggregateWhereApplication.class)
            .withPropertyValues(
                    "spring.datasource.url=jdbc:h2:mem:agg_where_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                    "spring.datasource.driver-class-name=org.h2.Driver",
                    "spring.datasource.username=sa",
                    "spring.datasource.password=",
                    "spring.jpa.hibernate.ddl-auto=create-drop",
                    "spring.jpa.open-in-view=false",
                    "spring.application.name=aggregate-where-test"
            );

    @Test
    void shouldApplyDefaultAndWhereBeforeAggregation() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            AggUserRepo repo = context.getBean(AggUserRepo.class);
            seed(repo);

            AggUserQuery query = new AggUserQuery();
            query.setName("A");
            query.setTitle("t2");

            query.agg(AggUserCalc.class)
                    .groupBy(AggUser::getName)
                    .field(AggUser::getName).to(AggUserCalc::getName)
                    .avg(AggUser::getAge).to(AggUserCalc::getAvgAge);

            List<AggUserCalc> rows = repo.<AggUserCalc>aggregate(query);

            assertThat(rows).hasSize(1);
            assertThat(rows.getFirst().getName()).isEqualTo("A");
            assertThat(rows.getFirst().getAvgAge()).isEqualTo(30.0d);
        });
    }

    @Test
    void shouldKeepWhereArrangementAndAggregationBothEffective() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            AggUserRepo repo = context.getBean(AggUserRepo.class);
            seed(repo);

            AggUserQuery query = new AggUserQuery();
            query.setName("A");
            query.setTitle("t1");
            query.setAgeGe(20);

            query.where()
                    .use(AggUserQuery::getName)
                    .or(AggUserQuery::getAgeGe);

            query.agg(AggUserCalc.class)
                    .groupBy(AggUser::getName)
                    .field(AggUser::getName).to(AggUserCalc::getName)
                    .avg(AggUser::getAge).to(AggUserCalc::getAvgAge);

            List<AggUserCalc> rows = repo.<AggUserCalc>aggregate(query).stream()
                    .sorted(Comparator.comparing(AggUserCalc::getName))
                    .toList();

            assertThat(rows).hasSize(2);
            assertThat(rows.get(0).getName()).isEqualTo("A");
            assertThat(rows.get(0).getAvgAge()).isEqualTo(20.0d);
            assertThat(rows.get(1).getName()).isEqualTo("B");
            assertThat(rows.get(1).getAvgAge()).isEqualTo(30.0d);
        });
    }

    @Test
    void shouldSupportSumCountAndCountDistinctTogether() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            AggUserRepo repo = context.getBean(AggUserRepo.class);
            repo.deleteAll();
            repo.saveAll(List.of(
                    new AggUser("A", "t1", 10),
                    new AggUser("A", "t2", 30),
                    new AggUser("A", "t3", 30),
                    new AggUser("B", "t1", 20)
            ));

            AggUserQuery query = new AggUserQuery();
            query.agg(AggUserCalc.class)
                    .groupBy(AggUser::getName)
                    .field(AggUser::getName).to(AggUserCalc::getName)
                    .sum(AggUser::getAge).to(AggUserCalc::getSumAge)
                    .count(AggUser::getAge).to(AggUserCalc::getCountAge)
                    .countDistinct(AggUser::getAge).to(AggUserCalc::getDistinctAgeCount);

            List<AggUserCalc> rows = repo.<AggUserCalc>aggregate(query).stream()
                    .sorted(Comparator.comparing(AggUserCalc::getName))
                    .toList();

            assertThat(rows).hasSize(2);
            assertThat(rows.get(0).getName()).isEqualTo("A");
            assertThat(rows.get(0).getSumAge()).isEqualTo(70.0d);
            assertThat(rows.get(0).getCountAge()).isEqualTo(3L);
            assertThat(rows.get(0).getDistinctAgeCount()).isEqualTo(2L);
            assertThat(rows.get(1).getName()).isEqualTo("B");
            assertThat(rows.get(1).getSumAge()).isEqualTo(20.0d);
            assertThat(rows.get(1).getCountAge()).isEqualTo(1L);
            assertThat(rows.get(1).getDistinctAgeCount()).isEqualTo(1L);
        });
    }

    @Test
    void shouldRejectUsingNormalSelectFlowWhenAggConfigured() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            AggUserRepo repo = context.getBean(AggUserRepo.class);
            seed(repo);

            AggUserQuery query = new AggUserQuery();
            query.setName("A");
            query.agg(AggUserCalc.class)
                    .groupBy(AggUser::getName)
                    .field(AggUser::getName).to(AggUserCalc::getName);

            assertThatThrownBy(() -> repo.loads(query))
                    .isInstanceOf(InvalidDataAccessApiUsageException.class)
                    .hasMessageContaining("aggregate()");
        });
    }

    private void seed(AggUserRepo repo) {

        repo.deleteAll();
        repo.saveAll(List.of(
                new AggUser("A", "t1", 10),
                new AggUser("A", "t2", 30),
                new AggUser("B", "t1", 20),
                new AggUser("B", "t2", 40)
        ));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class AggregateWhereApplication {
    }
}

@Entity
class AggUser extends JpaEntity {

    private String name;

    private String title;

    private Integer age;

    AggUser() {
    }

    AggUser(String name, String title, Integer age) {

        this.name = name;
        this.title = title;
        this.age = age;
    }

    public String getName() {

        return name;
    }

    public Integer getAge() {

        return age;
    }
}

interface AggUserRepo extends BaseRepository<AggUser> {
}

class AggUserQuery extends DslQuery<AggUser> {

    @Condition(field = "name")
    private String name;

    @Condition(field = "title")
    private String title;

    @Condition(type = QueryType.GE, field = "age")
    private Integer ageGe;

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

    public Integer getAgeGe() {

        return ageGe;
    }

    public void setAgeGe(Integer ageGe) {

        this.ageGe = ageGe;
    }
}

class AggUserCalc {

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

    public Long getCountAge() {

        return countAge;
    }

    public Long getDistinctAgeCount() {

        return distinctAgeCount;
    }
}
