package org.example.commonlib.jpa.write;

import com.dev.lib.jpa.entity.JpaEntity;
import com.dev.lib.jpa.entity.write.RepositoryWriteContext;
import com.dev.lib.jpa.entity.write.RepositoryWritePlugin;
import com.dev.lib.jpa.multiple.JpaDialect;
import com.dev.lib.jpa.multiple.JpaDatasource;
import org.example.commonlib.jpa.write.doris.DorisWriteEntity;
import org.example.commonlib.jpa.write.doris.DorisWriteRepo;
import org.example.commonlib.jpa.write.h2.H2WriteEntity;
import org.example.commonlib.jpa.write.h2.H2WriteRepo;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class RepositoryWritePluginIntegrationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withUserConfiguration(WritePluginApplication.class)
            .withPropertyValues(
                    "spring.jpa.hibernate.ddl-auto=create-drop",
                    "spring.jpa.open-in-view=false",
                    "spring.application.name=repository-write-plugin-test"
            );

    @Test
    void savePluginShouldReplaceOnlyRepositoriesBoundToMatchingLogicalDialect() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            H2WriteRepo h2Repo = context.getBean(H2WriteRepo.class);
            DorisWriteRepo dorisRepo = context.getBean(DorisWriteRepo.class);
            TrackingDorisWritePlugin plugin = context.getBean(TrackingDorisWritePlugin.class);

            H2WriteEntity h2Entity = h2Repo.save(new H2WriteEntity("h2"));
            DorisWriteEntity dorisEntity = dorisRepo.save(new DorisWriteEntity("doris"));

            assertThat(h2Entity.getId()).isPositive();
            assertThat(dorisEntity.getId()).isEqualTo(TrackingDorisWritePlugin.SYNTHETIC_ID);
            assertThat(plugin.saveCalls()).isEqualTo(1);
            assertThat(plugin.saveAllCalls()).isZero();
            assertThat(plugin.seenDialects()).containsExactly(JpaDialect.DORIS);
            assertThat(plugin.seenDatasourceNames()).containsExactly("dorisDs");
        });
    }

    @Test
    void saveAllPluginShouldReplaceOnlyRepositoriesBoundToMatchingLogicalDialect() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            H2WriteRepo h2Repo = context.getBean(H2WriteRepo.class);
            DorisWriteRepo dorisRepo = context.getBean(DorisWriteRepo.class);
            TrackingDorisWritePlugin plugin = context.getBean(TrackingDorisWritePlugin.class);

            List<H2WriteEntity> h2Saved = h2Repo.saveAll(List.of(new H2WriteEntity("h2-a"), new H2WriteEntity("h2-b")));
            List<DorisWriteEntity> dorisSaved = dorisRepo.saveAll(List.of(new DorisWriteEntity("doris-a"), new DorisWriteEntity("doris-b")));

            assertThat(h2Saved).extracting(JpaEntity::getId).allSatisfy(id -> assertThat(id).isPositive());
            assertThat(dorisSaved).extracting(JpaEntity::getId).containsExactly(
                    TrackingDorisWritePlugin.SYNTHETIC_ID,
                    TrackingDorisWritePlugin.SYNTHETIC_ID
            );
            assertThat(plugin.saveCalls()).isZero();
            assertThat(plugin.saveAllCalls()).isEqualTo(1);
            assertThat(plugin.seenDialects()).containsExactly(JpaDialect.DORIS);
            assertThat(plugin.seenDatasourceNames()).containsExactly("dorisDs");
        });
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @JpaDatasource(datasource = "h2Ds", packages = "org.example.commonlib.jpa.write.h2", dialect = JpaDialect.H2)
    @JpaDatasource(datasource = "dorisDs", packages = "org.example.commonlib.jpa.write.doris", dialect = JpaDialect.DORIS)
    static class WritePluginApplication {

        @Bean("h2Ds")
        @Primary
        DataSource h2DataSource() {

            return createDataSource("write_plugin_h2", "");
        }

        @Bean("dorisDs")
        DataSource dorisDataSource() {

            return createDataSource("write_plugin_doris", ";MODE=MySQL");
        }

        @Bean
        TrackingDorisWritePlugin trackingDorisWritePlugin() {

            return new TrackingDorisWritePlugin();
        }

        private DataSource createDataSource(String name, String options) {

            DriverManagerDataSource ds = new DriverManagerDataSource();
            ds.setDriverClassName("org.h2.Driver");
            ds.setUrl("jdbc:h2:mem:" + name + options + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
            ds.setUsername("sa");
            ds.setPassword("");
            return ds;
        }
    }

    static class TrackingDorisWritePlugin implements RepositoryWritePlugin {

        static final long SYNTHETIC_ID = -9_001L;

        private final AtomicInteger saveCalls = new AtomicInteger();

        private final AtomicInteger saveAllCalls = new AtomicInteger();

        private final List<JpaDialect> seenDialects = new ArrayList<>();

        private final List<String> seenDatasourceNames = new ArrayList<>();

        @Override
        public boolean supports(RepositoryWriteContext<?> context) {

            return context.logicalDialect() == JpaDialect.DORIS;
        }

        @Override
        public <T extends JpaEntity, S extends T> S save(RepositoryWriteContext<T> context, S entity) {

            saveCalls.incrementAndGet();
            record(context);
            entity.setId(SYNTHETIC_ID);
            return entity;
        }

        @Override
        public <T extends JpaEntity, S extends T> List<S> saveAll(RepositoryWriteContext<T> context, Iterable<S> entities) {

            saveAllCalls.incrementAndGet();
            record(context);
            List<S> saved = new ArrayList<>();
            for (S entity : entities) {
                entity.setId(SYNTHETIC_ID);
                saved.add(entity);
            }
            return saved;
        }

        int saveCalls() {

            return saveCalls.get();
        }

        int saveAllCalls() {

            return saveAllCalls.get();
        }

        List<JpaDialect> seenDialects() {

            return seenDialects;
        }

        List<String> seenDatasourceNames() {

            return seenDatasourceNames;
        }

        private void record(RepositoryWriteContext<?> context) {

            seenDialects.add(context.logicalDialect());
            seenDatasourceNames.add(context.datasourceName());
        }
    }
}
