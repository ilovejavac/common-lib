package org.example.commonlib.jpa.batch;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.jpa.entity.BaseRepository;
import com.dev.lib.jpa.entity.JpaEntity;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class SaveAllPersistenceContextIntegrationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withUserConfiguration(BatchSaveApplication.class)
            .withPropertyValues(
                    "spring.datasource.url=jdbc:h2:mem:batch_save_context;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                    "spring.datasource.driver-class-name=org.h2.Driver",
                    "spring.datasource.username=sa",
                    "spring.datasource.password=",
                    "spring.jpa.hibernate.ddl-auto=create-drop",
                    "spring.jpa.open-in-view=false",
                    "spring.jpa.properties.hibernate.jdbc.batch_size=1",
                    "spring.application.name=batch-save-context-test"
            );

    @Test
    void saveAllShouldNotDetachUnrelatedManagedEntitiesInOuterTransaction() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            BatchThingRepo repo = context.getBean(BatchThingRepo.class);
            TransactionTemplate transactionTemplate = context.getBean(TransactionTemplate.class);
            AtomicLong trackedId = new AtomicLong();

            transactionTemplate.executeWithoutResult(status -> {
                BatchThing tracked = repo.save(new BatchThing("before"));
                trackedId.set(tracked.getId());

                tracked.setName("flushed-before-save-all");
                repo.saveAll(List.of(new BatchThing("batch")));
                tracked.setName("changed-after-save-all");
            });

            assertThat(repo.load(new BatchThingQuery().setId(trackedId.get())))
                    .map(BatchThing::getName)
                    .contains("changed-after-save-all");
        });
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class BatchSaveApplication {
    }
}

@Entity
class BatchThing extends JpaEntity {

    private String name;

    public BatchThing() {
    }

    BatchThing(String name) {

        this.name = name;
    }

    String getName() {

        return name;
    }

    void setName(String name) {

        this.name = name;
    }
}

class BatchThingQuery extends DslQuery<BatchThing> {
}

interface BatchThingRepo extends BaseRepository<BatchThing> {
}
