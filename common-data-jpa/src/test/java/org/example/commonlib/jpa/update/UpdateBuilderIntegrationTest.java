package org.example.commonlib.jpa.update;

import com.dev.lib.entity.dsl.Condition;
import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.jpa.entity.BaseRepository;
import com.dev.lib.jpa.entity.JpaEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UpdateBuilderIntegrationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withUserConfiguration(UpdateBuilderApplication.class)
            .withPropertyValues(
                    "spring.datasource.url=jdbc:h2:mem:update_builder;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                    "spring.datasource.driver-class-name=org.h2.Driver",
                    "spring.datasource.username=sa",
                    "spring.datasource.password=",
                    "spring.jpa.hibernate.ddl-auto=create-drop",
                    "spring.jpa.open-in-view=false",
                    "spring.application.name=update-builder-test"
            );

    @Test
    void updateShouldAtomicallyChangeOnlyMatchingActiveRows() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            UpdateItemRepo repository = context.getBean(UpdateItemRepo.class);
            JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);

            UpdateItem first = repository.save(item("update-1", "init"));
            UpdateItem second = repository.save(item("update-2", "init"));

            long affected = repository.update()
                    .set(UpdateItem::getStatus, "changed")
                    .where(new UpdateItemQuery().setBizId(first.getBizId()))
                    .execute();

            assertThat(affected).isEqualTo(1);
            assertThat(queryStatus(jdbcTemplate, first.getId())).isEqualTo("changed");
            assertThat(queryStatus(jdbcTemplate, second.getId())).isEqualTo("init");
        });
    }

    @Test
    void updateShouldSkipSoftDeletedRows() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            UpdateItemRepo repository = context.getBean(UpdateItemRepo.class);
            JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);

            UpdateItem deleted = repository.save(item("update-3", "init"));
            repository.delete(deleted);

            long affected = repository.update()
                    .set(UpdateItem::getStatus, "changed")
                    .where(new UpdateItemQuery().setBizId(deleted.getBizId()))
                    .execute();

            assertThat(affected).isZero();
            assertThat(queryStatus(jdbcTemplate, deleted.getId())).isEqualTo("init");
        });
    }

    @Test
    void updateShouldRejectMissingBusinessCondition() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            UpdateItemRepo repository = context.getBean(UpdateItemRepo.class);

            assertThatThrownBy(() -> repository.update()
                    .set(UpdateItem::getStatus, "changed")
                    .execute())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("业务条件");
            assertThatThrownBy(() -> repository.update()
                    .set(UpdateItem::getStatus, "changed")
                    .where(new UpdateItemQuery())
                    .execute())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("业务条件");
        });
    }

    @Test
    void updateShouldRejectEmptyAssignments() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            UpdateItemRepo repository = context.getBean(UpdateItemRepo.class);

            assertThatThrownBy(() -> repository.update()
                    .where(new UpdateItemQuery().setBizId("missing"))
                    .execute())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("至少设置一个字段");
        });
    }

    private static UpdateItem item(String bizId, String status) {

        UpdateItem item = new UpdateItem();
        item.setBizId(bizId);
        item.setStatus(status);
        return item;
    }

    private static String queryStatus(JdbcTemplate jdbcTemplate, Long id) {

        return jdbcTemplate.queryForObject("select status from update_item where id = ?", String.class, id);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class UpdateBuilderApplication {
    }
}

@Entity
@Table(name = "update_item")
class UpdateItem extends JpaEntity {

    private String status;

    String getStatus() {

        return status;
    }

    void setStatus(String status) {

        this.status = status;
    }
}

interface UpdateItemRepo extends BaseRepository<UpdateItem> {
}

class UpdateItemQuery extends DslQuery<UpdateItem> {

    @Condition(field = "bizId")
    private String bizId;

    public UpdateItemQuery setBizId(String bizId) {

        this.bizId = bizId;
        return this;
    }
}
