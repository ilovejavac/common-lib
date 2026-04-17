package org.example.commonlib.jpa.batch;

import com.dev.lib.jpa.entity.BaseRepository;
import com.dev.lib.jpa.entity.JpaEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import java.util.ArrayList;
import java.util.List;

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
