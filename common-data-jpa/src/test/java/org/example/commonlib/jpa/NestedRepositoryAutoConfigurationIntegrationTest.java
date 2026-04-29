package org.example.commonlib.jpa;

import com.dev.lib.jpa.entity.BaseRepository;
import com.dev.lib.jpa.entity.JpaEntity;
import com.dev.lib.entity.dsl.DslQuery;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;

class NestedRepositoryAutoConfigurationIntegrationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withUserConfiguration(NestedRepositoryApplication.class)
            .withPropertyValues(
                    "spring.datasource.url=jdbc:h2:mem:nested_repository_auto_config;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                    "spring.datasource.driver-class-name=org.h2.Driver",
                    "spring.datasource.username=sa",
                    "spring.datasource.password=",
                    "spring.jpa.hibernate.ddl-auto=create-drop",
                    "spring.jpa.open-in-view=false",
                    "spring.application.name=nested-repository-auto-config-test"
            );

    @Test
    void shouldAutoRegisterNestedBaseRepositoryWithSingleDatasource() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(NestedDataSource.Mapper.class);
            assertThat(context).hasSingleBean(NestedDataSource.UseCase.class);

            NestedDataSource.UseCase useCase = context.getBean(NestedDataSource.UseCase.class);
            useCase.createLoadAndDelete("single-nested");
        });
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class NestedRepositoryApplication {

        @Bean
        NestedDataSource.UseCase nestedDataSourceUseCase(NestedDataSource.Mapper mapper) {

            return new NestedDataSource.UseCase(mapper);
        }
    }
}

class NestedDataSource {

    private NestedDataSource() {
    }

    @jakarta.persistence.Entity
    @Table(name = "nested_repo_entity")
    static class Entity extends JpaEntity {

        private String name;

        String getName() {

            return name;
        }

        void setName(String name) {

            this.name = name;
        }
    }

    interface Mapper extends BaseRepository<Entity> {
    }

    static class UseCase {

        private final Mapper mapper;

        UseCase(Mapper mapper) {

            this.mapper = mapper;
        }

        void createLoadAndDelete(String name) {

            Entity entity = new Entity();
            entity.setName(name);
            mapper.saveAndFlush(entity);

            assertThat(mapper.load(new Load().setName(name)))
                    .hasValueSatisfying(loaded -> assertThat(loaded.getName()).isEqualTo(name));

            mapper.delete(new Load().setName(name));

            assertThat(mapper.count()).isZero();
            assertThat(mapper.onlyDeleted().count()).isEqualTo(1);
        }
    }

    static class Load extends DslQuery<Entity> {

        private String name;

        String getName() {

            return name;
        }

        Load setName(String name) {

            this.name = name;
            return this;
        }
    }
}
