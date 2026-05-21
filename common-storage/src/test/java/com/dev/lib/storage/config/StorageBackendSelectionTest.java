package com.dev.lib.storage.config;

import com.dev.lib.storage.data.SysFileObjectRepository;
import com.dev.lib.storage.domain.service.StorageServiceNameProvider;
import com.dev.lib.storage.domain.service.chain.LocalChainStorage;
import com.dev.lib.storage.domain.service.chain.MinioChainStorage;
import com.dev.lib.storage.domain.service.chain.OssChainStorage;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class StorageBackendSelectionTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
            .withUserConfiguration(StorageTestConfig.class)
            .withBean(SysFileObjectRepository.class, () -> mock(SysFileObjectRepository.class))
            .withBean(StorageServiceNameProvider.class, () -> new StorageServiceNameProvider("test-service"));

    @Test
    void shouldAutoRegisterLocalStorageWhenOnlyLocalPathIsConfigured() {

        contextRunner
                .withPropertyValues("app.storage.local.path=storage")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(LocalChainStorage.class);
                    assertThat(context).doesNotHaveBean(MinioChainStorage.class);
                    assertThat(context).doesNotHaveBean(OssChainStorage.class);
                });
    }

    @Test
    void shouldAutoRegisterMinioStorageWhenOnlyMinioIsConfigured() {

        contextRunner
                .withPropertyValues(
                        "app.storage.minio.endpoint=http://127.0.0.1:9000",
                        "app.storage.minio.access-key=test-access",
                        "app.storage.minio.secret-key=test-secret",
                        "app.storage.minio.bucket=test-bucket"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(MinioChainStorage.class);
                    assertThat(context).doesNotHaveBean(LocalChainStorage.class);
                    assertThat(context).doesNotHaveBean(OssChainStorage.class);
                });
    }

    @Test
    void shouldFailWhenMultipleBackendsAreConfiguredWithoutExplicitType() {

        contextRunner
                .withPropertyValues(
                        "app.storage.local.path=storage",
                        "app.storage.minio.endpoint=http://127.0.0.1:9000",
                        "app.storage.minio.access-key=test-access",
                        "app.storage.minio.secret-key=test-secret",
                        "app.storage.minio.bucket=test-bucket"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .rootCause()
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("app.storage.type must be configured when multiple storage backends are configured");
                });
    }

    @Test
    void shouldFailWhenExplicitTypeBackendIsNotFullyConfigured() {

        contextRunner
                .withPropertyValues(
                        "app.storage.type=minio",
                        "app.storage.local.path=storage"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .rootCause()
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("app.storage.minio must be fully configured when app.storage.type=minio");
                });
    }

    @Configuration
    @Import({
            StorageAutoConfig.class,
            LocalChainStorage.class,
            MinioChainStorage.class,
            OssChainStorage.class
    })
    static class StorageTestConfig {
    }
}
