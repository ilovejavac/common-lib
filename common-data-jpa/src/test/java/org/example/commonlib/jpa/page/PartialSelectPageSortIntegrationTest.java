package org.example.commonlib.jpa.page;

import com.dev.lib.entity.dsl.Condition;
import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.jpa.entity.BaseRepository;
import com.dev.lib.jpa.entity.JpaEntity;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.data.domain.Page;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class PartialSelectPageSortIntegrationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withUserConfiguration(PartialSelectPageSortApplication.class)
            .withPropertyValues(
                    "spring.datasource.url=jdbc:h2:mem:partial_select_page_sort;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                    "spring.datasource.driver-class-name=org.h2.Driver",
                    "spring.datasource.username=sa",
                    "spring.datasource.password=",
                    "spring.jpa.hibernate.ddl-auto=create-drop",
                    "spring.jpa.open-in-view=false",
                    "spring.jpa.show-sql=true",
                    "spring.application.name=partial-select-page-sort-test"
            );

    @Test
    void shouldSupportSelectDtoPageWithSortString() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            PageSortUserRepo repo = context.getBean(PageSortUserRepo.class);
            repo.saveAll(List.of(
                    new PageSortUser("A"),
                    new PageSortUser("B"),
                    new PageSortUser("C")
            ));

            PageSortUserQuery query = new PageSortUserQuery();
            query.setSortStr("name_desc");
            query.setOffset(1);
            query.setLimit(2);

            Page<PageSortUserDto> page = repo.select(PageSortUser::getName)
                    .page(PageSortUserDto.class, query);

            assertThat(page.getContent()).hasSize(2);
            assertThat(page.getContent().get(0).getName()).isEqualTo("B");
            assertThat(page.getContent().get(1).getName()).isEqualTo("A");
        });
    }

    @Test
    void selectDtoPageWithoutDslQueryShouldApplyDefaultPageLimit() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            PageSortUserRepo repo = context.getBean(PageSortUserRepo.class);
            repo.saveAll(buildUsers("default-page-", 130));

            Page<PageSortUserDto> page = repo.select(PageSortUser::getName)
                    .page(PageSortUserDto.class, null);

            assertThat(page.getContent()).hasSize(128);
            assertThat(page.getTotalElements()).isEqualTo(130);
        });
    }

    @Test
    void fullEntityPageShouldKeepTotalWhenRequestedPageIsEmpty() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            PageSortUserRepo repo = context.getBean(PageSortUserRepo.class);
            repo.saveAll(List.of(
                    new PageSortUser("A"),
                    new PageSortUser("B"),
                    new PageSortUser("C")
            ));

            PageSortUserQuery query = new PageSortUserQuery();
            query.setOffset(10);
            query.setLimit(2);

            Page<PageSortUser> page = repo.page(query);

            assertThat(page.getContent()).isEmpty();
            assertThat(page.getTotalElements()).isEqualTo(3);
        });
    }

    @Test
    void fullEntityStreamShouldApplyDslLimitAndOffset() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            PageSortUserRepo repo = context.getBean(PageSortUserRepo.class);
            repo.saveAll(List.of(
                    new PageSortUser("A"),
                    new PageSortUser("B"),
                    new PageSortUser("C")
            ));

            PageSortUserQuery query = new PageSortUserQuery();
            query.setSortStr("name_desc");
            query.setOffset(1);
            query.setLimit(2);

            TransactionTemplate transactionTemplate = context.getBean(TransactionTemplate.class);
            List<String> names = transactionTemplate.execute(status -> {
                try (Stream<PageSortUser> stream = repo.stream(query)) {
                    return stream.map(PageSortUser::getName).toList();
                }
            });

            assertThat(names).containsExactly("B", "A");
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
    static class PartialSelectPageSortApplication {
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

    public String getName() {

        return name;
    }

    public void setName(String name) {

        this.name = name;
    }
}

class PageSortUserDto {

    private String name;

    public String getName() {

        return name;
    }
}
