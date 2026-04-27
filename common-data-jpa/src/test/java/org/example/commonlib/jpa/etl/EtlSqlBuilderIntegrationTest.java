package org.example.commonlib.jpa.etl;

import com.dev.lib.jpa.entity.BaseRepository;
import com.dev.lib.jpa.entity.EtlSqlStatementResult;
import com.dev.lib.jpa.entity.JpaEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EtlSqlBuilderIntegrationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withUserConfiguration(EtlSqlBuilderApplication.class)
            .withPropertyValues(
                    "spring.datasource.url=jdbc:h2:mem:etl_sql_builder_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                    "spring.datasource.driver-class-name=org.h2.Driver",
                    "spring.datasource.username=sa",
                    "spring.datasource.password=",
                    "spring.jpa.hibernate.ddl-auto=create-drop",
                    "spring.jpa.open-in-view=false",
                    "spring.application.name=etl-sql-builder-test"
            );

    @Test
    void executeShouldRunAllowedEtlScriptAgainstRepositoryDatasource() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            EtlAnchorRepo repo = context.getBean(EtlAnchorRepo.class);
            JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);

            List<EtlSqlStatementResult> results = repo.etl("""
                    CREATE TABLE etl_source (
                        source_id bigint primary key,
                        source_name varchar(32),
                        score int
                    );
                    CREATE TABLE etl_target (
                        target_id bigint,
                        target_name varchar(32)
                    );
                    INSERT INTO etl_source
                    SELECT 1, 'alpha;still-one-value', 10;
                    INSERT INTO etl_target
                    SELECT source_id, source_name
                    FROM etl_source
                    WHERE score > 5;
                    SELECT * FROM etl_target;
                    """).execute();

            assertThat(results).hasSize(5);
            assertThat(results).extracting(EtlSqlStatementResult::statementType)
                    .containsExactly("CREATE_TABLE", "CREATE_TABLE", "INSERT_INTO_SELECT", "INSERT_INTO_SELECT", "SELECT");
            assertThat(results.get(3).updateCount()).isEqualTo(1);
            assertThat(results.get(4).resultSet()).isTrue();

            assertThat(jdbcTemplate.queryForObject("select count(*) from etl_target", Long.class)).isEqualTo(1L);
            assertThat(jdbcTemplate.queryForObject("select target_name from etl_target", String.class))
                    .isEqualTo("alpha;still-one-value");
        });
    }

    @Test
    void executeShouldValidateWholeScriptBeforeRunningAnyStatement() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            EtlAnchorRepo repo = context.getBean(EtlAnchorRepo.class);
            JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);

            assertThatThrownBy(() -> repo.etl("""
                    CREATE TABLE etl_should_not_exist (id bigint);
                    DELETE FROM etl_anchor;
                    """).execute())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("不允许执行");

            Long tableCount = jdbcTemplate.queryForObject("""
                    select count(*)
                    from information_schema.tables
                    where table_name = 'ETL_SHOULD_NOT_EXIST'
                    """, Long.class);
            assertThat(tableCount).isZero();
        });
    }

    @Test
    void executeShouldRejectInsertValues() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            EtlAnchorRepo repo = context.getBean(EtlAnchorRepo.class);

            assertThatThrownBy(() -> repo.etl("""
                    CREATE TABLE etl_values_target (id bigint);
                    INSERT INTO etl_values_target VALUES (1);
                    """).execute())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("INSERT INTO 只允许 SELECT");
        });
    }

    @Test
    void dropShouldRenameTableByDefault() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            EtlAnchorRepo repo = context.getBean(EtlAnchorRepo.class);
            JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);

            jdbcTemplate.execute("create table etl_wrong_table (id bigint)");

            EtlSqlStatementResult result = repo.drop("etl_wrong_table").execute();

            assertThat(result.statementType()).isEqualTo("RENAME_TABLE");
            Long originalTableCount = jdbcTemplate.queryForObject("""
                    select count(*)
                    from information_schema.tables
                    where table_name = 'ETL_WRONG_TABLE'
                    """, Long.class);
            Long backupTableCount = jdbcTemplate.queryForObject("""
                    select count(*)
                    from information_schema.tables
                    where table_name = 'BACK_ETL_WRONG_TABLE'
                    """, Long.class);
            assertThat(originalTableCount).isZero();
            assertThat(backupTableCount).isEqualTo(1L);
        });
    }

    @Test
    void dropShouldPhysicallyDropBackupTableWhenEnabled() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            EtlAnchorRepo repo = context.getBean(EtlAnchorRepo.class);
            JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);

            jdbcTemplate.execute("create table etl_physical_wrong_table (id bigint)");

            EtlSqlStatementResult result = repo.drop("etl_physical_wrong_table")
                    .physical(true)
                    .execute();

            assertThat(result.statementType()).isEqualTo("DROP_TABLE");
            Long originalTableCount = jdbcTemplate.queryForObject("""
                    select count(*)
                    from information_schema.tables
                    where table_name = 'ETL_PHYSICAL_WRONG_TABLE'
                    """, Long.class);
            Long backupTableCount = jdbcTemplate.queryForObject("""
                    select count(*)
                    from information_schema.tables
                    where table_name = 'BACK_ETL_PHYSICAL_WRONG_TABLE'
                    """, Long.class);
            assertThat(originalTableCount).isZero();
            assertThat(backupTableCount).isZero();
        });
    }

    @Test
    void dropShouldRejectSqlInsteadOfTableName() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            EtlAnchorRepo repo = context.getBean(EtlAnchorRepo.class);

            assertThatThrownBy(() -> repo.drop("drop table etl_anchor").execute())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("只允许传入单张表名");

            assertThatThrownBy(() -> repo.drop("etl_anchor; drop table other_table").execute())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("只允许传入单张表名");
        });
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class EtlSqlBuilderApplication {
    }
}

@Entity
@Table(name = "etl_anchor")
class EtlAnchor extends JpaEntity {
}

interface EtlAnchorRepo extends BaseRepository<EtlAnchor> {
}
