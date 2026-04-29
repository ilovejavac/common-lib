package org.example.commonlib.jpa.batch;

import com.dev.lib.jpa.entity.BaseRepository;
import com.dev.lib.jpa.entity.JpaEntity;
import com.dev.lib.entity.dsl.DslQuery;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class DeleteBatchExecutionIntegrationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withUserConfiguration(DeleteBatchExecutionApplication.class)
            .withPropertyValues(
                    "spring.datasource.url=jdbc:h2:mem:delete_batch_execution;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                    "spring.datasource.driver-class-name=org.h2.Driver",
                    "spring.datasource.username=sa",
                    "spring.datasource.password=",
                    "spring.jpa.hibernate.ddl-auto=create-drop",
                    "spring.jpa.open-in-view=false",
                    "spring.jpa.show-sql=true",
                    "spring.jpa.properties.hibernate.generate_statistics=true",
                    "spring.jpa.properties.hibernate.session_factory.statement_inspector=org.example.commonlib.jpa.batch.DeleteBatchExecutionIntegrationTest$SqlCaptureInspector",
                    "spring.application.name=delete-batch-execution-test"
            );

    @Test
    void softDeleteAllByIdShouldUseInClauseBatches() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            DeleteBatchThingRepo repo = context.getBean(DeleteBatchThingRepo.class);
            List<Long> ids = repo.saveAll(buildEntities("soft-", 2050))
                    .stream()
                    .map(DeleteBatchThing::getId)
                    .toList();

            Statistics statistics = context.getBean(EntityManagerFactory.class)
                    .unwrap(SessionFactory.class)
                    .getStatistics();
            statistics.clear();

            repo.deleteAllById(ids);

            long preparedStatementCount = statistics.getPrepareStatementCount();
            assertThat(preparedStatementCount).isEqualTo(3);
            assertThat(repo.onlyDeleted().count()).isEqualTo(2050);
        });
    }

    @Test
    void softDeleteAllShouldUseChunkedIdScanAndInClauseBatches() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            DeleteBatchThingRepo repo = context.getBean(DeleteBatchThingRepo.class);
            repo.saveAll(buildEntities("soft-all-", 2050));

            Statistics statistics = context.getBean(EntityManagerFactory.class)
                    .unwrap(SessionFactory.class)
                    .getStatistics();
            statistics.clear();

            repo.deleteAll();

            long preparedStatementCount = statistics.getPrepareStatementCount();
            assertThat(preparedStatementCount).isEqualTo(7);
            assertThat(repo.onlyDeleted().count()).isEqualTo(2050);
        });
    }

    @Test
    void softDeleteByDslQueryShouldUseSingleBulkUpdate() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            DeleteBatchThingRepo repo = context.getBean(DeleteBatchThingRepo.class);
            repo.saveAll(buildEntities("soft-query-", 2050));

            Statistics statistics = context.getBean(EntityManagerFactory.class)
                    .unwrap(SessionFactory.class)
                    .getStatistics();
            statistics.clear();

            repo.delete(new DeleteBatchThingQuery().setNameStartWith("soft-query-"));

            long preparedStatementCount = statistics.getPrepareStatementCount();
            assertThat(preparedStatementCount).isEqualTo(1);
            assertThat(repo.onlyDeleted().count()).isEqualTo(2050);
        });
    }

    @Test
    void softDeleteByDslQueryShouldNotDuplicateActiveDeletedPredicate() {

        SqlCaptureInspector.clear();
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            DeleteBatchThingRepo repo = context.getBean(DeleteBatchThingRepo.class);
            repo.saveAll(buildEntities("soft-query-sql-", 2));
            SqlCaptureInspector.clear();

            repo.delete(new DeleteBatchThingQuery().setNameStartWith("soft-query-sql-"));

            String updateSql = SqlCaptureInspector.statements().stream()
                    .filter(sql -> sql.toLowerCase(Locale.ROOT).startsWith("update delete_batch_thing "))
                    .findFirst()
                    .orElseThrow();
            assertThat(countOccurrences(updateSql.toLowerCase(Locale.ROOT), "deleted"))
                    .isEqualTo(2);
        });
    }

    @Test
    void softDeleteByOnlyDeletedDslQueryShouldNotGenerateContradictoryDeletedPredicates() {

        SqlCaptureInspector.clear();
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            DeleteBatchThingRepo repo = context.getBean(DeleteBatchThingRepo.class);
            repo.saveAll(buildEntities("only-deleted-query-", 2));
            SqlCaptureInspector.clear();

            repo.onlyDeleted().delete(new DeleteBatchThingQuery().setNameStartWith("only-deleted-query-"));

            List<String> updateSqls = SqlCaptureInspector.statements().stream()
                    .filter(sql -> sql.toLowerCase(Locale.ROOT).startsWith("update delete_batch_thing "))
                    .toList();
            assertThat(updateSqls).isEmpty();
            assertThat(repo.count(new DeleteBatchThingQuery().setNameStartWith("only-deleted-query-")))
                    .isEqualTo(2);
            assertThat(repo.onlyDeleted().count(new DeleteBatchThingQuery().setNameStartWith("only-deleted-query-")))
                    .isZero();
        });
    }

    @Test
    void physicalDeleteAllByIdShouldUseInClauseBatches() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            DeleteBatchThingRepo repo = context.getBean(DeleteBatchThingRepo.class);
            List<Long> ids = repo.saveAll(buildEntities("hard-", 2050))
                    .stream()
                    .map(DeleteBatchThing::getId)
                    .toList();

            Statistics statistics = context.getBean(EntityManagerFactory.class)
                    .unwrap(SessionFactory.class)
                    .getStatistics();
            statistics.clear();

            repo.physicalDelete().deleteAllById(ids);

            long preparedStatementCount = statistics.getPrepareStatementCount();
            assertThat(preparedStatementCount).isEqualTo(3);
            assertThat(repo.withDeleted().count()).isEqualTo(0);
        });
    }

    @Test
    void physicalDeleteByPredicateShouldUseChunkedIdScanAndInClauseBatches() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            DeleteBatchThingRepo repo = context.getBean(DeleteBatchThingRepo.class);
            repo.saveAll(buildEntities("hard-predicate-", 2050));

            Statistics statistics = context.getBean(EntityManagerFactory.class)
                    .unwrap(SessionFactory.class)
                    .getStatistics();
            statistics.clear();

            long affected = repo.physicalDelete().delete();

            long preparedStatementCount = statistics.getPrepareStatementCount();
            assertThat(affected).isEqualTo(2050);
            assertThat(preparedStatementCount).isEqualTo(7);
            assertThat(repo.withDeleted().count()).isEqualTo(0);
        });
    }

    private static List<DeleteBatchThing> buildEntities(String prefix, int size) {

        List<DeleteBatchThing> entities = new ArrayList<>(size);
        for (int i = 1; i <= size; i++) {
            entities.add(new DeleteBatchThing(prefix + i));
        }
        return entities;
    }

    private static int countOccurrences(String text, String target) {

        int count = 0;
        int index = 0;
        while ((index = text.indexOf(target, index)) >= 0) {
            count++;
            index += target.length();
        }
        return count;
    }

    public static class SqlCaptureInspector implements StatementInspector {

        private static final List<String> STATEMENTS = new CopyOnWriteArrayList<>();

        static void clear() {

            STATEMENTS.clear();
        }

        static List<String> statements() {

            return List.copyOf(STATEMENTS);
        }

        @Override
        public String inspect(String sql) {

            STATEMENTS.add(sql);
            return sql;
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class DeleteBatchExecutionApplication {
    }
}

@Entity
class DeleteBatchThing extends JpaEntity {

    private String name;

    public DeleteBatchThing() {
    }

    DeleteBatchThing(String name) {

        this.name = name;
    }
}

interface DeleteBatchThingRepo extends BaseRepository<DeleteBatchThing> {
}

class DeleteBatchThingQuery extends DslQuery<DeleteBatchThing> {

    private String nameStartWith;

    DeleteBatchThingQuery setNameStartWith(String nameStartWith) {

        this.nameStartWith = nameStartWith;
        return this;
    }
}
