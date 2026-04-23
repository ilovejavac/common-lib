package org.example.commonlib.jpa.update;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.entity.encrypt.Encrypt;
import com.dev.lib.jpa.entity.BaseRepository;
import com.dev.lib.jpa.entity.JpaEntity;
import com.dev.lib.security.util.SecurityContextHolder;
import com.dev.lib.security.util.UserDetails;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateBuilderIntegrationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withUserConfiguration(UpdateBuilderApplication.class)
            .withPropertyValues(
                    "spring.datasource.url=jdbc:h2:mem:update_builder_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                    "spring.datasource.driver-class-name=org.h2.Driver",
                    "spring.datasource.username=sa",
                    "spring.datasource.password=",
                    "spring.jpa.hibernate.ddl-auto=create-drop",
                    "spring.jpa.open-in-view=false",
                    "spring.application.name=update-builder-test",
                    "app.security.encrypt-version=base64"
            );

    @Test
    void setWithNullShouldBeSkippedAndSetNullShouldWriteDatabaseNull() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            UpdateCaseRepo repo = context.getBean(UpdateCaseRepo.class);
            UpdateCaseThing saved = repo.saveAndFlush(new UpdateCaseThing("before", PlanState.WAITING, "remark-1", "plain-before"));

            UpdateCaseQuery query = new UpdateCaseQuery();
            query.setId(saved.getId());

            long skipped = repo.update()
                    .set(UpdateCaseThing::getRemark, null)
                    .where(query)
                    .execute();

            assertThat(skipped).isEqualTo(1L);
            assertThat(repo.findById(saved.getId())).map(UpdateCaseThing::getRemark).contains("remark-1");

            long nulled = repo.update()
                    .setNull(UpdateCaseThing::getRemark)
                    .where(query)
                    .execute();

            assertThat(nulled).isEqualTo(1L);
            assertThat(repo.findById(saved.getId())).map(UpdateCaseThing::getRemark).isEmpty();
        });
    }

    @Test
    void shouldSupportEnumJsonEncryptAndAutoAuditFields() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            UpdateCaseRepo repo = context.getBean(UpdateCaseRepo.class);
            JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);

            UpdateCaseThing saved = repo.saveAndFlush(new UpdateCaseThing("before", PlanState.WAITING, "remark-2", "plain-before"));
            LocalDateTime oldUpdatedAt = saved.getUpdatedAt();

            UpdateCaseQuery query = new UpdateCaseQuery();
            query.setId(saved.getId());

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("code", "A-1");
            payload.put("count", 2);

            SecurityContextHolder.with(UserDetails.builder().id(101L).username("u101").build(), () -> {
                long affected = repo.update()
                        .set(UpdateCaseThing::getName, "after")
                        .set(UpdateCaseThing::getExecuteState, PlanState.RUNNING)
                        .set(UpdateCaseThing::getPayload, payload)
                        .set(UpdateCaseThing::getSecretText, "plain-secret")
                        .where(query)
                        .execute();
                assertThat(affected).isEqualTo(1L);
            });

            UpdateCaseThing reloaded = repo.findById(saved.getId()).orElseThrow();
            assertThat(reloaded.getName()).isEqualTo("after");
            assertThat(reloaded.getExecuteState()).isEqualTo(PlanState.RUNNING);
            assertThat(reloaded.getPayload()).containsEntry("code", "A-1").containsEntry("count", 2);
            assertThat(reloaded.getSecretText()).isEqualTo("plain-secret");
            assertThat(reloaded.getModifierId()).isEqualTo(101L);
            assertThat(reloaded.getUpdatedAt()).isAfter(oldUpdatedAt);

            Map<String, Object> row = jdbcTemplate.queryForMap(
                    "select execute_state, payload, secret_text from update_case_thing where id = ?",
                    saved.getId()
            );
            assertThat(row.get("execute_state")).isEqualTo("RUNNING");
            assertThat(String.valueOf(row.get("payload"))).contains("\"code\":\"A-1\"");
            assertThat(String.valueOf(row.get("secret_text"))).startsWith("v1:").isNotEqualTo("plain-secret");
        });
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class UpdateBuilderApplication {
    }
}

@Entity
@Table(name = "update_case_thing")
class UpdateCaseThing extends JpaEntity {

    @Column(length = 32)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(length = 12, columnDefinition = "varchar(12)")
    private PlanState executeState;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "text")
    private Map<String, Object> payload;

    @Column(length = 64)
    private String remark;

    @Encrypt
    @Column(length = 256)
    private String secretText;

    UpdateCaseThing() {
    }

    UpdateCaseThing(String name, PlanState executeState, String remark, String secretText) {

        this.name = name;
        this.executeState = executeState;
        this.remark = remark;
        this.secretText = secretText;
    }

    public String getName() {

        return name;
    }

    public PlanState getExecuteState() {

        return executeState;
    }

    public Map<String, Object> getPayload() {

        return payload;
    }

    public String getRemark() {

        return remark;
    }

    public String getSecretText() {

        return secretText;
    }
}

enum PlanState {
    WAITING,
    RUNNING
}

interface UpdateCaseRepo extends BaseRepository<UpdateCaseThing> {
}

class UpdateCaseQuery extends DslQuery<UpdateCaseThing> {
}
