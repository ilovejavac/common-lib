package org.example.commonlib.jpa;

import com.dev.lib.jpa.TransactionHelper;
import com.dev.lib.jpa.entity.BaseRepository;
import com.dev.lib.jpa.entity.BaseRepositoryImpl;
import com.dev.lib.jpa.entity.JpaEntity;
import com.dev.lib.jpa.entity.RepositoryUtils;
import com.dev.lib.jpa.entity.write.RepositoryWriteContext;
import com.dev.lib.jpa.multiple.JpaDialect;
import com.dev.lib.jpa.multiple.JpaDatasource;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
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

    @Test
    void shouldApplyDatabasePlatformPerDatasource() {

        new WebApplicationContextRunner()
                .withUserConfiguration(MultiDatasourceDialectApplication.class)
                .withPropertyValues(
                        "spring.jpa.hibernate.ddl-auto=none",
                        "spring.jpa.open-in-view=false",
                        "spring.application.name=multi-jpa-datasource-dialect-test",
                        "app.dialect=H2",
                        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("sqliteDsEntityManagerFactory");
                    assertThat(context).hasBean("pgsqlDsEntityManagerFactory");

                    EntityManagerFactory sqliteEmf = context.getBean("sqliteDsEntityManagerFactory", EntityManagerFactory.class);
                    EntityManagerFactory pgsqlEmf = context.getBean("pgsqlDsEntityManagerFactory", EntityManagerFactory.class);

                    assertThat(sqliteEmf.getProperties().get("hibernate.dialect"))
                            .isEqualTo("org.hibernate.community.dialect.SQLiteDialect");
                    assertThat(pgsqlEmf.getProperties().get("hibernate.dialect"))
                            .isEqualTo("org.hibernate.dialect.PostgreSQLDialect");
                    assertThat(sqliteEmf.getProperties().get(RepositoryWriteContext.DATASOURCE_NAME_PROPERTY))
                            .isEqualTo("sqliteDs");
                    assertThat(sqliteEmf.getProperties().get(RepositoryWriteContext.LOGICAL_DIALECT_PROPERTY))
                            .isEqualTo("SQLITE");
                    assertThat(pgsqlEmf.getProperties().get(RepositoryWriteContext.DATASOURCE_NAME_PROPERTY))
                            .isEqualTo("pgsqlDs");
                    assertThat(pgsqlEmf.getProperties().get(RepositoryWriteContext.LOGICAL_DIALECT_PROPERTY))
                            .isEqualTo("POSTGRESQL");
                });
    }

    @Test
    void shouldApplyAppDialectForMultiDatasourceWithoutExplicitDialect() {

        new WebApplicationContextRunner()
                .withUserConfiguration(MultiDatasourceApplication.class)
                .withPropertyValues(
                        "spring.jpa.hibernate.ddl-auto=none",
                        "spring.jpa.open-in-view=false",
                        "spring.application.name=multi-jpa-datasource-ignore-app-dialect-test",
                        "app.dialect=SQLITE"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    EntityManagerFactory primaryEmf = context.getBean("primaryDsEntityManagerFactory", EntityManagerFactory.class);
                    EntityManagerFactory archiveEmf = context.getBean("archiveDsEntityManagerFactory", EntityManagerFactory.class);

                    assertThat(resolveDialect(primaryEmf))
                            .isEqualTo("org.hibernate.community.dialect.SQLiteDialect");
                    assertThat(resolveDialect(archiveEmf))
                            .isEqualTo("org.hibernate.community.dialect.SQLiteDialect");
                });
    }

    @Test
    void shouldApplySharedHikariDefaultsToManagedJpaDatasourcesOnly(CapturedOutput output) {

        new WebApplicationContextRunner()
                .withUserConfiguration(HikariManagedDatasourceApplication.class)
                .withPropertyValues(
                        "spring.jpa.hibernate.ddl-auto=none",
                        "spring.jpa.open-in-view=false",
                        "spring.application.name=multi-jpa-hikari-defaults-test",
                        "app.jpa.hikari.maximum-pool-size=24",
                        "app.jpa.hikari.minimum-idle=8",
                        "app.jpa.hikari.connection-timeout=3000",
                        "app.jpa.hikari.validation-timeout=1000",
                        "app.jpa.hikari.idle-timeout=600000",
                        "app.jpa.hikari.max-lifetime=1740000",
                        "app.jpa.hikari.keepalive-time=300000"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    DataSource primaryDs = context.getBean("primaryDs", DataSource.class);
                    DataSource archiveDs = context.getBean("archiveDs", DataSource.class);
                    DataSource ignoredDs = context.getBean("ignoredDs", DataSource.class);

                    HikariDataSource primaryHikari = unwrapHikari(primaryDs);
                    HikariDataSource archiveHikari = unwrapHikari(archiveDs);
                    HikariDataSource ignoredHikari = unwrapHikari(ignoredDs);

                    assertThat(primaryHikari.getMaximumPoolSize()).isEqualTo(24);
                    assertThat(primaryHikari.getMinimumIdle()).isEqualTo(8);
                    assertThat(primaryHikari.getConnectionTimeout()).isEqualTo(3000);
                    assertThat(primaryHikari.getValidationTimeout()).isEqualTo(1000);
                    assertThat(primaryHikari.getIdleTimeout()).isEqualTo(600000);
                    assertThat(primaryHikari.getMaxLifetime()).isEqualTo(1740000);
                    assertThat(primaryHikari.getKeepaliveTime()).isEqualTo(300000);

                    assertThat(archiveHikari.getMaximumPoolSize()).isEqualTo(30);
                    assertThat(archiveHikari.getMinimumIdle()).isEqualTo(3);
                    assertThat(archiveHikari.getConnectionTimeout()).isEqualTo(3000);
                    assertThat(archiveHikari.getValidationTimeout()).isEqualTo(1000);
                    assertThat(archiveHikari.getIdleTimeout()).isEqualTo(600000);
                    assertThat(archiveHikari.getMaxLifetime()).isEqualTo(1740000);
                    assertThat(archiveHikari.getKeepaliveTime()).isEqualTo(300000);

                    assertThat(ignoredHikari.getMaximumPoolSize()).isEqualTo(10);
                    assertThat(ignoredHikari.getMinimumIdle()).isEqualTo(-1);
                    assertThat(ignoredHikari.getConnectionTimeout()).isEqualTo(30000);
                    assertThat(ignoredHikari.getValidationTimeout()).isEqualTo(5000);
                    assertThat(ignoredHikari.getIdleTimeout()).isEqualTo(600000);
                    assertThat(ignoredHikari.getMaxLifetime()).isEqualTo(1800000);
                    assertThat(ignoredHikari.getKeepaliveTime()).isEqualTo(120000);
                    assertThat(output.getAll())
                            .doesNotContain("not eligible for getting processed by all BeanPostProcessors");
                });
    }

    @Test
    void shouldSkipNonHikariManagedDatasource() {

        new WebApplicationContextRunner()
                .withUserConfiguration(MultiDatasourceApplication.class)
                .withPropertyValues(
                        "spring.jpa.hibernate.ddl-auto=none",
                        "spring.jpa.open-in-view=false",
                        "spring.application.name=multi-jpa-non-hikari-test",
                        "app.jpa.hikari.maximum-pool-size=24"
                )
                .run(context -> assertThat(context).hasNotFailed());
    }

    private static HikariDataSource unwrapHikari(DataSource dataSource) {

        try {
            return dataSource.unwrap(HikariDataSource.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to unwrap HikariDataSource from DataSource bean", e);
        }
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

    private static String resolveDialect(EntityManagerFactory emf) {

        return emf.unwrap(SessionFactoryImplementor.class)
                .getJdbcServices()
                .getDialect()
                .getClass()
                .getName();
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

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @JpaDatasource(datasource = "primaryDs", packages = "org.example.commonlib.jpa")
    @JpaDatasource(datasource = "archiveDs", packages = "org.example.commonlib.jpa.archive")
    static class HikariManagedDatasourceApplication {

        @Bean("primaryDs")
        @Primary
        HikariDataSource primaryDataSource() {

            return createDataSource("primary_ds");
        }

        @Bean("archiveDs")
        HikariDataSource archiveDataSource() {

            HikariDataSource ds = createDataSource("archive_ds");
            ds.setMaximumPoolSize(30);
            ds.setMinimumIdle(3);
            return ds;
        }

        @Bean("ignoredDs")
        HikariDataSource ignoredDataSource() {

            return createDataSource("ignored_ds");
        }

        private HikariDataSource createDataSource(String name) {

            HikariDataSource ds = new HikariDataSource();
            ds.setDriverClassName("org.h2.Driver");
            ds.setJdbcUrl("jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
            ds.setUsername("sa");
            ds.setPassword("");
            return ds;
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @JpaDatasource(
            datasource = "sqliteDs",
            packages = "org.example.commonlib.jpa.sqlite",
            dialect = JpaDialect.SQLITE
    )
    @JpaDatasource(
            datasource = "pgsqlDs",
            packages = "org.example.commonlib.jpa.pgsql",
            dialect = JpaDialect.POSTGRESQL
    )
    static class MultiDatasourceDialectApplication {

        @Bean("sqliteDs")
        @Primary
        DataSource sqliteDataSource() {

            return createDataSource("sqlite_ds");
        }

        @Bean("pgsqlDs")
        DataSource pgsqlDataSource() {

            return createDataSource("pgsql_ds");
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
