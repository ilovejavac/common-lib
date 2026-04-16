package org.example.commonlib.jpa;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class SingleDatasourceDialectIntegrationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withUserConfiguration(SingleDatasourceDialectApplication.class)
            .withPropertyValues(
                    "spring.datasource.url=jdbc:h2:mem:single_datasource_dialect;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                    "spring.datasource.driver-class-name=org.h2.Driver",
                    "spring.datasource.username=sa",
                    "spring.datasource.password=",
                    "spring.jpa.hibernate.ddl-auto=none",
                    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                    "app.dialect=SQLITE",
                    "spring.application.name=single-datasource-dialect-test"
            );

    @Test
    void shouldApplyAppDialectForSingleDatasource() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            EntityManagerFactory emf = context.getBean(EntityManagerFactory.class);
            assertThat(emf.getProperties().get("hibernate.dialect"))
                    .isEqualTo("org.hibernate.community.dialect.SQLiteDialect");
        });
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class SingleDatasourceDialectApplication {
    }
}
