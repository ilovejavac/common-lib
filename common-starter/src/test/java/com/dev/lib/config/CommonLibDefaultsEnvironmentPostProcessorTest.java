package com.dev.lib.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CommonLibDefaultsEnvironmentPostProcessorTest {

    @Test
    void shouldLoadCommonLibDefaultsAndKeepBusinessOverride() {

        ConfigurableEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("business", Map.of(
                "common.lib.auto-import-test.override", "business"
        )));

        new CommonLibDefaultsEnvironmentPostProcessor()
                .postProcessEnvironment(environment, new SpringApplication(Object.class));

        assertThat(environment.getProperty("common.lib.auto-import-test.only-in-lib"))
                .isEqualTo("loaded");
        assertThat(environment.getProperty("common.lib.auto-import-test.override"))
                .isEqualTo("business");

        MutablePropertySources sources = environment.getPropertySources();
        List<String> sourceNames = java.util.stream.StreamSupport.stream(sources.spliterator(), false)
                .map(ps -> ps.getName())
                .toList();

        int businessIndex = sourceNames.indexOf("business");
        int libSourceIndex = indexOfLibSource(sourceNames);
        assertThat(libSourceIndex).isGreaterThan(businessIndex);
    }

    @Test
    void shouldNotLoadDangerousDeploymentAndSecurityDefaults() {

        ConfigurableEnvironment environment = new StandardEnvironment();

        new CommonLibDefaultsEnvironmentPostProcessor()
                .postProcessEnvironment(environment, new SpringApplication(Object.class));

        assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isNull();
        assertThat(environment.getProperty("spring.kafka.bootstrap-servers")).isNull();
        assertThat(environment.getProperty("spring.rabbitmq.virtual-host")).isNull();
        assertThat(environment.getProperty("spring.rocketmq.name-server")).isNull();
        assertThat(environment.getProperty("app.opensearch.hosts")).isNull();
        assertThat(environment.getProperty("app.security.secret")).isNull();
    }

    @Test
    void shouldSupportDisablingAutoImport() {

        ConfigurableEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("switch", Map.of(
                "app.common-lib.auto-config.enabled", "false"
        )));

        new CommonLibDefaultsEnvironmentPostProcessor()
                .postProcessEnvironment(environment, new SpringApplication(Object.class));

        assertThat(environment.getProperty("common.lib.auto-import-test.only-in-lib")).isNull();
    }

    private int indexOfLibSource(List<String> sourceNames) {

        for (int i = 0; i < sourceNames.size(); i++) {
            if (sourceNames.get(i).startsWith("common-lib-auto-config:")) {
                return i;
            }
        }
        return -1;
    }
}
