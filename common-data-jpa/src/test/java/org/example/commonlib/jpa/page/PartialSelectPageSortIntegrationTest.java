package org.example.commonlib.jpa.page;

import com.dev.lib.entity.dsl.Condition;
import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.entity.dsl.QueryType;
import com.dev.lib.jpa.Bo;
import com.dev.lib.jpa.entity.BaseRepository;
import com.dev.lib.jpa.entity.JpaEntity;
import com.dev.lib.web.model.QueryRequest;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.data.domain.Page;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class PartialSelectPageSortIntegrationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withUserConfiguration(RepositoryReadApplication.class)
            .withPropertyValues(
                    "spring.datasource.url=jdbc:h2:mem:repository_read;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                    "spring.datasource.driver-class-name=org.h2.Driver",
                    "spring.datasource.username=sa",
                    "spring.datasource.password=",
                    "spring.jpa.hibernate.ddl-auto=create-drop",
                    "spring.jpa.open-in-view=false",
                    "spring.jpa.show-sql=false",
                    "spring.application.name=repository-read-test"
            );

    @Test
    void loadShouldReturnOneMatchingEntity() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            PageSortUserRepo repo = context.getBean(PageSortUserRepo.class);
            repo.saveAll(List.of(new PageSortUser("A"), new PageSortUser("B")));

            assertThat(repo.load(new PageSortUserQuery().setName("B")))
                    .map(PageSortUser::getName)
                    .contains("B");
        });
    }

    @Test
    void lockForUpdateShouldLoadInsideTransaction() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            PageSortUserRepo repo = context.getBean(PageSortUserRepo.class);
            repo.save(new PageSortUser("locked"));
            TransactionTemplate transaction = context.getBean(TransactionTemplate.class);

            String name = transaction.execute(status -> {
                assertThat(repo.loadForUpdate(new PageSortUserQuery().setName("locked")))
                        .map(PageSortUser::getName)
                        .contains("locked");
                String updateLocked = repo.lockForUpdate()
                        .load(new PageSortUserQuery().setName("locked"))
                        .map(PageSortUser::getName)
                        .orElseThrow();
                assertThat(repo.lockForShare().loads(new PageSortUserQuery().setName("locked")))
                        .extracting(PageSortUser::getName)
                        .containsExactly("locked");
                return updateLocked;
            });

            assertThat(name).isEqualTo("locked");
        });
    }

    @Test
    void restoredReadOperationsShouldHonorDeletedScopesAndDslBounds() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            PageSortUserRepo repo = context.getBean(PageSortUserRepo.class);
            repo.save(new PageSortUser("active"));
            PageSortUser deleted = repo.save(new PageSortUser("deleted"));
            repo.delete(new Bo<>(deleted));

            assertThat(repo.count()).isEqualTo(1);
            assertThat(repo.exists(new PageSortUserQuery().setName("active"))).isTrue();
            assertThat(repo.exists(new PageSortUserQuery().setName("deleted"))).isFalse();
            assertThat(repo.onlyDeleted().count()).isEqualTo(1);
            assertThat(repo.onlyDeleted().exists(new PageSortUserQuery().setName("deleted"))).isTrue();
            assertThat(repo.withDeleted().count()).isEqualTo(2);

            PageSortUserQuery streamQuery = new PageSortUserQuery();
            streamQuery.setSortStr("name_desc");
            streamQuery.setOffset(1);
            streamQuery.setLimit(1);
            TransactionTemplate transaction = context.getBean(TransactionTemplate.class);
            List<String> names = transaction.execute(status -> {
                try (Stream<PageSortUser> stream = repo.withDeleted().stream(streamQuery)) {
                    return stream.map(PageSortUser::getName).toList();
                }
            });

            assertThat(names).containsExactly("active");
        });
    }

    @Test
    void entityEqualityShouldWorkWithHibernateProxy() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            PageSortUserRepo repo = context.getBean(PageSortUserRepo.class);
            PageSortUser saved = repo.save(new PageSortUser("proxy"));
            EntityManager entityManager = context.getBean(EntityManager.class);
            TransactionTemplate transaction = context.getBean(TransactionTemplate.class);

            PageSortUser reference = transaction.execute(status ->
                    entityManager.getReference(PageSortUser.class, saved.getId())
            );

            assertThat(reference).isEqualTo(saved);
            assertThat(saved).isEqualTo(reference);
            assertThat(reference).hasSameHashCodeAs(saved);
        });
    }

    @Test
    void loadsWithBusinessConditionsShouldReturnAllMatchingRows(CapturedOutput output) {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            PageSortUserRepo repo = context.getBean(PageSortUserRepo.class);
            repo.saveAll(buildUsers("filtered-", 530));

            PageSortUserQuery query = new PageSortUserQuery();
            query.setNameStartWith("filtered-");

            assertThat(repo.loads(query)).hasSize(530);
            assertThat(output).doesNotContain("loads result reached its limit");
        });
    }

    @Test
    void loadsShouldHonorExplicitLimit(CapturedOutput output) {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            PageSortUserRepo repo = context.getBean(PageSortUserRepo.class);
            repo.saveAll(buildUsers("limited-", 720));

            PageSortUserQuery query = new PageSortUserQuery();
            query.setNameStartWith("limited-");
            query.setLimit(700);

            assertThat(repo.loads(query)).hasSize(700);
            assertThat(output).contains("loads result reached its limit");
        });
    }

    @Test
    void loadsShouldNotWarnWhenResultExactlyMatchesExplicitLimit(CapturedOutput output) {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            PageSortUserRepo repo = context.getBean(PageSortUserRepo.class);
            repo.saveAll(buildUsers("exact-", 700));

            PageSortUserQuery query = new PageSortUserQuery();
            query.setNameStartWith("exact-");
            query.setLimit(700);

            assertThat(repo.loads(query)).hasSize(700);
            assertThat(output).doesNotContain("loads result reached its limit");
        });
    }

    @Test
    void loadsWithoutConditionsShouldCapAtFindAllLimit(CapturedOutput output) {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            PageSortUserRepo repo = context.getBean(PageSortUserRepo.class);
            repo.saveAll(buildUsers("findall-", 10241));

            assertThat(repo.loads()).hasSize(10240);
            assertThat(repo.loads(new PageSortUserQuery().setLimit(20000))).hasSize(10240);
            assertThat(repo.loads(new PageSortUserQuery().setLimit(100))).hasSize(100);
            assertThat(output).contains("loads result reached its limit")
                    .contains("size=10241")
                    .contains("limit=10240");
        });
    }

    @Test
    void loadsWithoutConditionsShouldNotWarnWhenTotalEqualsFindAllLimit(CapturedOutput output) {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            PageSortUserRepo repo = context.getBean(PageSortUserRepo.class);
            repo.saveAll(buildUsers("exact-findall-", 10240));

            assertThat(repo.loads()).hasSize(10240);
            assertThat(output).doesNotContain("loads result reached its limit");
        });
    }

    @Test
    void emptyInAndNotInShouldHaveDeterministicSemantics() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            PageSortUserRepo repo = context.getBean(PageSortUserRepo.class);
            repo.saveAll(List.of(new PageSortUser("A"), new PageSortUser("B")));

            PageSortUserQuery emptyIn = new PageSortUserQuery();
            emptyIn.setNameIn(List.of());
            assertThat(repo.loads(emptyIn)).isEmpty();

            PageSortUserQuery emptyNotIn = new PageSortUserQuery();
            emptyNotIn.setNameNotIn(List.of());
            assertThat(repo.loads(emptyNotIn)).extracting(PageSortUser::getName)
                    .containsExactlyInAnyOrder("A", "B");
        });
    }

    @Test
    void pageWithoutQueryShouldKeepDefaultSizeAndTotal() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            PageSortUserRepo repo = context.getBean(PageSortUserRepo.class);
            repo.saveAll(buildUsers("default-page-", 530));

            Page<PageSortUser> page = repo.page(null);

            assertThat(page.getContent()).hasSize(512);
            assertThat(page.getTotalElements()).isEqualTo(530);
        });
    }

    @Test
    void pageWithQueryRequestShouldKeepRequestDefaults() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            PageSortUserRepo repo = context.getBean(PageSortUserRepo.class);
            repo.saveAll(buildUsers("request-page-", 24));

            QueryRequest<PageSortUserQuery> request = new QueryRequest<>();
            request.setQuery(new PageSortUserQuery());
            PageSortUserQuery query = new PageSortUserQuery();
            query.external(request);

            Page<PageSortUser> page = repo.page(query);

            assertThat(page.getContent()).hasSize(20);
            assertThat(page.getTotalElements()).isEqualTo(24);
            assertThat(page.getPageable().getPageNumber()).isZero();
            assertThat(page.getPageable().getPageSize()).isEqualTo(20);
            assertThat(page.hasNext()).isTrue();
        });
    }

    @Test
    void pageShouldKeepExistingOneBasedOffsetSemantics() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            PageSortUserRepo repo = context.getBean(PageSortUserRepo.class);
            repo.saveAll(List.of(
                    new PageSortUser("A"),
                    new PageSortUser("B"),
                    new PageSortUser("C"),
                    new PageSortUser("D"),
                    new PageSortUser("E")
            ));

            PageSortUserQuery query = new PageSortUserQuery();
            query.setSortStr("name_desc");
            query.setOffset(3);
            query.setLimit(2);

            Page<PageSortUser> page = repo.page(query);

            assertThat(page.getContent()).extracting(PageSortUser::getName).containsExactly("A");
            assertThat(page.getTotalElements()).isEqualTo(5);
        });
    }

    @Test
    void pageShouldKeepTotalWhenRequestedPageIsEmpty() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            PageSortUserRepo repo = context.getBean(PageSortUserRepo.class);
            repo.saveAll(List.of(new PageSortUser("A"), new PageSortUser("B"), new PageSortUser("C")));

            PageSortUserQuery query = new PageSortUserQuery();
            query.setOffset(10);
            query.setLimit(2);

            Page<PageSortUser> page = repo.page(query);

            assertThat(page.getContent()).isEmpty();
            assertThat(page.getTotalElements()).isEqualTo(3);
        });
    }

    private static List<PageSortUser> buildUsers(String prefix, int size) {

        List<PageSortUser> users = new ArrayList<>(size);
        for (int i = 1; i <= size; i++) {
            users.add(new PageSortUser(prefix + i));
        }
        return users;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class RepositoryReadApplication {
    }
}

@Entity
class PageSortUser extends JpaEntity {

    private String name;

    PageSortUser(String name) {

        this.name = name;
    }

    public PageSortUser() {
    }

    public String getName() {

        return name;
    }
}

interface PageSortUserRepo extends BaseRepository<PageSortUser> {
}

class PageSortUserQuery extends DslQuery<PageSortUser> {

    @Condition(field = "name")
    private String name;

    @Condition(field = "name", type = QueryType.START_WITH)
    private String nameStartWith;

    @Condition(field = "name", type = QueryType.IN)
    private Collection<String> nameIn;

    @Condition(field = "name", type = QueryType.NOT_IN)
    private Collection<String> nameNotIn;

    public String getName() {

        return name;
    }

    public PageSortUserQuery setName(String name) {

        this.name = name;
        return this;
    }

    public String getNameStartWith() {

        return nameStartWith;
    }

    public void setNameStartWith(String nameStartWith) {

        this.nameStartWith = nameStartWith;
    }

    public Collection<String> getNameIn() {

        return nameIn;
    }

    public void setNameIn(Collection<String> nameIn) {

        this.nameIn = nameIn;
    }

    public Collection<String> getNameNotIn() {

        return nameNotIn;
    }

    public void setNameNotIn(Collection<String> nameNotIn) {

        this.nameNotIn = nameNotIn;
    }
}
