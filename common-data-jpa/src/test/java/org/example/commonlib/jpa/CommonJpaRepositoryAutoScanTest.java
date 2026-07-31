package org.example.commonlib.jpa;

import com.dev.lib.jpa.entity.log.OperateLogRepo;
import com.dev.lib.testsupport.repository.CommonLibNestedLedger;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CommonJpaRepositoryAutoScanTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withUserConfiguration(BusinessApplication.class)
            .withPropertyValues(
                    "spring.datasource.url=jdbc:h2:mem:common_jpa_repo_scan;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                    "spring.datasource.driver-class-name=org.h2.Driver",
                    "spring.datasource.username=sa",
                    "spring.datasource.password=",
                    "spring.jpa.hibernate.ddl-auto=none",
                    "spring.application.name=common-jpa-repo-scan-test"
            );

    @Test
    void shouldAutoRegisterCommonAndBusinessRepositoriesWithoutManualJpaScanConfig() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(OperateLogRepo.class);
            assertThat(context).hasSingleBean(CommonLibNestedLedger.Mapper.class);
            assertThat(context).hasSingleBean(BusinessOrderRepo.class);
        });
    }

    @Test
    void applicationDataDefaultsShouldSetJpaBatchAndFetchSizes() throws Exception {

        List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                .load("application-data", new ClassPathResource("application-data.yaml"));

        assertThat(sources)
                .anySatisfy(source -> {
                    assertThat(source.getProperty("spring.jpa.properties.hibernate.default_batch_fetch_size"))
                            .isEqualTo(512);
                    assertThat(source.getProperty("spring.jpa.properties.hibernate.jdbc.fetch_size"))
                            .isEqualTo(512);
                    assertThat(source.getProperty("spring.jpa.properties.hibernate.jdbc.batch_size"))
                            .isEqualTo(128);
                });
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class BusinessApplication {
    }

}

@Entity
class BusinessOrder {

    @Id
    private Long id;

}

interface BusinessOrderRepo extends JpaRepository<BusinessOrder, Long> {
}
