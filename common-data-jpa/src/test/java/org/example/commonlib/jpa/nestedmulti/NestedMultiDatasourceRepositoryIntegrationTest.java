package org.example.commonlib.jpa.nestedmulti;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.jpa.entity.BaseRepository;
import com.dev.lib.jpa.entity.JpaEntity;
import com.dev.lib.jpa.multiple.JpaDatasource;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

class NestedMultiDatasourceRepositoryIntegrationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withUserConfiguration(NestedMultiDatasourceApplication.class)
            .withPropertyValues(
                    "spring.jpa.hibernate.ddl-auto=create-drop",
                    "spring.jpa.open-in-view=false",
                    "spring.application.name=nested-multi-datasource-repository-test",
                    "app.jpa.hikari.maximum-pool-size=24",
                    "app.jpa.hikari.minimum-idle=8",
                    "app.jpa.hikari.connection-timeout=3000"
            );

    @Test
    void shouldRegisterAndUseNestedRepositoryWithManagedDatasource() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(NestedManagedDataSource.Mapper.class);
            assertThat(context).hasSingleBean(NestedManagedDataSource.UseCase.class);

            HikariDataSource hikariDataSource = unwrapHikari(context.getBean("nestedPrimaryDs", DataSource.class));
            assertThat(hikariDataSource.getMaximumPoolSize()).isEqualTo(24);
            assertThat(hikariDataSource.getMinimumIdle()).isEqualTo(8);
            assertThat(hikariDataSource.getConnectionTimeout()).isEqualTo(3000);

            NestedManagedDataSource.UseCase useCase = context.getBean(NestedManagedDataSource.UseCase.class);
            useCase.createLoadAndDelete("multi-nested");
        });
    }

    private static HikariDataSource unwrapHikari(DataSource dataSource) {

        try {
            return dataSource.unwrap(HikariDataSource.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to unwrap HikariDataSource from DataSource bean", e);
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @JpaDatasource(datasource = "nestedPrimaryDs", packages = "org.example.commonlib.jpa.nestedmulti")
    static class NestedMultiDatasourceApplication {

        @Bean("nestedPrimaryDs")
        @Primary
        HikariDataSource nestedPrimaryDataSource() {

            HikariDataSource ds = new HikariDataSource();
            ds.setDriverClassName("org.h2.Driver");
            ds.setJdbcUrl("jdbc:h2:mem:nested_multi_ds;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
            ds.setUsername("sa");
            ds.setPassword("");
            return ds;
        }

        @Bean
        NestedManagedDataSource.UseCase nestedManagedDataSourceUseCase(NestedManagedDataSource.Mapper mapper) {

            return new NestedManagedDataSource.UseCase(mapper);
        }
    }
}

class NestedManagedDataSource {

    private NestedManagedDataSource() {
    }

    @jakarta.persistence.Entity
    @Table(name = "nested_managed_entity")
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
