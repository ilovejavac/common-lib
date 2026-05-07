package com.dev.lib.search;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class OpenSearchConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OpenSearchConfig.class));

    @Test
    void shouldRegisterPropertiesWhenOpenSearchIsEnabled() {

        contextRunner
                .withPropertyValues(
                        "app.opensearch.enabled=true",
                        "app.opensearch.index=orders"
                )
                .run(context -> assertThat(context)
                        .hasSingleBean(OpenSearchConfig.OpenSearchProperties.class)
                        .getBean(OpenSearchConfig.OpenSearchProperties.class)
                        .extracting(OpenSearchConfig.OpenSearchProperties::getIndex)
                        .isEqualTo("orders"));
    }

    @Test
    void shouldNotRegisterPropertiesWhenOpenSearchIsDisabled() {

        contextRunner.run(context -> assertThat(context)
                .doesNotHaveBean(OpenSearchConfig.OpenSearchProperties.class));
    }
}
