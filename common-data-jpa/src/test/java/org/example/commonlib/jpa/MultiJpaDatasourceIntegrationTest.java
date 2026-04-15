package org.example.commonlib.jpa;

import com.dev.lib.jpa.TransactionHelper;
import com.dev.lib.jpa.entity.BaseRepository;
import com.dev.lib.jpa.entity.BaseRepositoryImpl;
import com.dev.lib.jpa.entity.JpaEntity;
import com.dev.lib.jpa.entity.RepositoryUtils;
import com.dev.lib.jpa.multiple.JpaDatasource;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class MultiJpaDatasourceIntegrationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withUserConfiguration(MultiDatasourceApplication.class)
            .withPropertyValues(
                    "spring.jpa.hibernate.ddl-auto=none",
                    "spring.jpa.open-in-view=false",
                    "spring.application.name=multi-jpa-datasource-test"
            );

    @Test
    void shouldKeepBaseRepositoryImplAndResolveMatchedTransactionManagerInMultiDatasourceMode() throws Exception {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(MultiOrderRepo.class);
            assertThat(context).hasBean("primaryDsTransactionManager");
            assertThat(context).hasBean("archiveDsTransactionManager");
            assertThat(context).hasSingleBean(TransactionHelper.class);

            MultiOrderRepo repo = context.getBean(MultiOrderRepo.class);
            BaseRepositoryImpl<MultiOrder> impl = RepositoryUtils.unwrap(repo);
            assertThat(impl.getClass()).isEqualTo(BaseRepositoryImpl.class);

            EntityManagerFactory emf = extractEntityManagerFactory(impl);
            PlatformTransactionManager resolved = resolveTransactionManager(emf);
            assertThat(resolved).isSameAs(context.getBean("primaryDsTransactionManager"));
        });
    }

    private static EntityManagerFactory extractEntityManagerFactory(BaseRepositoryImpl<?> impl) {

        try {
            Method getter = BaseRepositoryImpl.class.getDeclaredMethod("getEntityManagerFactory");
            getter.setAccessible(true);
            return (EntityManagerFactory) getter.invoke(impl);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read EntityManagerFactory from BaseRepositoryImpl", e);
        }
    }

    private static PlatformTransactionManager resolveTransactionManager(EntityManagerFactory emf) {

        try {
            Method resolver = TransactionHelper.class.getDeclaredMethod("resolveTransactionManager", EntityManagerFactory.class);
            resolver.setAccessible(true);
            return (PlatformTransactionManager) resolver.invoke(null, emf);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to resolve transaction manager by EntityManagerFactory", e);
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @JpaDatasource(datasource = "primaryDs", packages = "org.example.commonlib.jpa")
    @JpaDatasource(datasource = "archiveDs", packages = "org.example.commonlib.jpa.archive")
    static class MultiDatasourceApplication {

        @Bean("primaryDs")
        @Primary
        DataSource primaryDataSource() {

            return createDataSource("primary_ds");
        }

        @Bean("archiveDs")
        DataSource archiveDataSource() {

            return createDataSource("archive_ds");
        }

        private DataSource createDataSource(String name) {

            DriverManagerDataSource ds = new DriverManagerDataSource();
            ds.setDriverClassName("org.h2.Driver");
            ds.setUrl("jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
            ds.setUsername("sa");
            ds.setPassword("");
            return ds;
        }
    }
}

@Entity
class MultiOrder extends JpaEntity {
}

interface MultiOrderRepo extends BaseRepository<MultiOrder> {
}
