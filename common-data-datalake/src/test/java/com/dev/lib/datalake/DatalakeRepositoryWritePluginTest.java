package com.dev.lib.datalake;

import com.dev.lib.datalake.config.DatalakeAutoConfig;
import com.dev.lib.datalake.config.DatalakeProperties;
import com.dev.lib.jpa.entity.JpaEntity;
import com.dev.lib.jpa.entity.write.RepositoryWriteContext;
import com.dev.lib.jpa.entity.write.RepositoryWritePlugin;
import com.dev.lib.jpa.entity.write.RepositoryWritePluginChain;
import com.dev.lib.jpa.entity.write.RepositoryWritePluginRegistrar;
import com.dev.lib.jpa.multiple.JpaDialect;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatalakeRepositoryWritePluginTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DatalakeAutoConfig.class))
            .withUserConfiguration(PluginRegistrarConfig.class)
            .withPropertyValues(
                    "app.datalake.doris.stream-load-urls[0]=http://doris-fe:8030",
                    "app.datalake.doris.username=root",
                    "app.datalake.doris.password=secret",
                    "app.datalake.doris.connect-timeout=3s",
                    "app.datalake.doris.database=ods",
                    "app.datalake.clickhouse.urls[0]=http://clickhouse:8123",
                    "app.datalake.hive.urls[0]=http://hive:10001"
            );

    @Test
    void autoConfigurationShouldBindDorisPropertiesAndRegisterSpringPlugins() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(DatalakeProperties.class);
            assertThat(context).hasSingleBean(DorisRepositoryWritePlugin.class);
            assertThat(context).hasSingleBean(ClickHouseRepositoryWritePlugin.class);
            assertThat(context).hasSingleBean(HiveRepositoryWritePlugin.class);

            DatalakeProperties properties = context.getBean(DatalakeProperties.class);

            assertThat(properties.getDoris().getStreamLoadUrls()).containsExactly("http://doris-fe:8030");
            assertThat(properties.getDoris().getUsername()).isEqualTo("root");
            assertThat(properties.getDoris().getDatabase()).isEqualTo("ods");

            assertThat(resolve(JpaDialect.DORIS, "dorisDs")).containsInstanceOf(DorisRepositoryWritePlugin.class);
        });
    }

    @Test
    void springPluginsShouldReplaceOnlyDatalakeDialectsAfterApplicationStartup() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            assertThat(resolve(JpaDialect.DORIS, "dorisDs")).containsInstanceOf(DorisRepositoryWritePlugin.class);
            assertThat(resolve(JpaDialect.CLICKHOUSE, "clickhouseDs")).containsInstanceOf(ClickHouseRepositoryWritePlugin.class);
            assertThat(resolve(JpaDialect.HIVE, "hiveDs")).containsInstanceOf(HiveRepositoryWritePlugin.class);
            assertThat(resolve(JpaDialect.POSTGRESQL, "pgDs")).isEmpty();
        });
    }

    @Test
    void defaultDatalakeSaveImplementationShouldFailFastUntilConcreteWriterIsProvided() {

        contextRunner.run(applicationContext -> {
            assertThat(applicationContext).hasNotFailed();

            RepositoryWriteContext<DemoEntity> context = context(JpaDialect.DORIS, "dorisDs");
            DemoEntity entity = new DemoEntity();
            RepositoryWritePlugin plugin = resolve(JpaDialect.DORIS, "dorisDs").orElseThrow();

            assertThatThrownBy(() -> plugin.save(context, entity))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("DORIS")
                    .hasMessageContaining("not implemented")
                    .hasMessageContaining("app.datalake.doris");

            assertThatThrownBy(() -> plugin.saveAll(context, List.of(entity)))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("DORIS")
                    .hasMessageContaining("not implemented")
                    .hasMessageContaining("app.datalake.doris");
        });
    }

    @Test
    void userSpringPluginShouldOverrideDefaultDatalakePluginByOrder() {

        contextRunner.withUserConfiguration(CustomDorisPluginConfig.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    RepositoryWritePlugin plugin = resolve(JpaDialect.DORIS, "dorisDs").orElseThrow();

                    assertThat(plugin).isInstanceOf(CustomDorisWritePlugin.class);
                });
    }

    private static Optional<RepositoryWritePlugin> resolve(JpaDialect dialect, String datasourceName) {

        return RepositoryWritePluginChain.getInstance().resolve(context(dialect, datasourceName));
    }

    private static RepositoryWriteContext<DemoEntity> context(JpaDialect dialect, String datasourceName) {

        return new RepositoryWriteContext<>(
                null,
                DemoEntity.class,
                null,
                null,
                datasourceName,
                dialect
        );
    }

    @Configuration(proxyBeanMethods = false)
    static class PluginRegistrarConfig {

        @Bean
        RepositoryWritePluginRegistrar repositoryWritePluginRegistrar(ObjectProvider<RepositoryWritePlugin> plugins) {

            return new RepositoryWritePluginRegistrar(plugins);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomDorisPluginConfig {

        @Bean
        CustomDorisWritePlugin customDorisWritePlugin() {

            return new CustomDorisWritePlugin();
        }
    }

    static class CustomDorisWritePlugin implements RepositoryWritePlugin {

        private final List<String> datasourceNames = new ArrayList<>();

        @Override
        public boolean supports(RepositoryWriteContext<?> context) {

            return context.logicalDialect() == JpaDialect.DORIS;
        }

        @Override
        public <T extends JpaEntity, S extends T> S save(RepositoryWriteContext<T> context, S entity) {

            datasourceNames.add(context.datasourceName());
            return entity;
        }

        @Override
        public <T extends JpaEntity, S extends T> List<S> saveAll(RepositoryWriteContext<T> context, Iterable<S> entities) {

            datasourceNames.add(context.datasourceName());
            List<S> saved = new ArrayList<>();
            entities.forEach(saved::add);
            return saved;
        }
    }

    static class DemoEntity extends JpaEntity {
    }
}
