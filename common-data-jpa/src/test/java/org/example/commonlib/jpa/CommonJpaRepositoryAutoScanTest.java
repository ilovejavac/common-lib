package org.example.commonlib.jpa;

import com.dev.lib.jpa.TransactionHelper;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.data.jpa.repository.JpaRepository;

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
//            assertThat(context).hasSingleBean(AuditRepo.class);
            assertThat(context).hasSingleBean(BusinessOrderRepo.class);
            assertThat(context).hasSingleBean(TransactionHelper.class);
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
