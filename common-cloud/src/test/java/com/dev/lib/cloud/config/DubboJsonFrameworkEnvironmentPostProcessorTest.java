package com.dev.lib.cloud.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class DubboJsonFrameworkEnvironmentPostProcessorTest {

    @Test
    void shouldPreferFastjson2ByDefault() {

        String previous = System.getProperty(DubboJsonFrameworkEnvironmentPostProcessor.PREFER_JSON_FRAMEWORK_KEY);
        try {
            System.clearProperty(DubboJsonFrameworkEnvironmentPostProcessor.PREFER_JSON_FRAMEWORK_KEY);

            new DubboJsonFrameworkEnvironmentPostProcessor()
                    .postProcessEnvironment(new StandardEnvironment(), new SpringApplication(Object.class));

            assertThat(System.getProperty(DubboJsonFrameworkEnvironmentPostProcessor.PREFER_JSON_FRAMEWORK_KEY))
                    .isEqualTo("fastjson2");
        } finally {
            restoreProperty(previous);
        }
    }

    @Test
    void shouldKeepExplicitJsonFrameworkPreference() {

        String previous = System.getProperty(DubboJsonFrameworkEnvironmentPostProcessor.PREFER_JSON_FRAMEWORK_KEY);
        try {
            System.setProperty(DubboJsonFrameworkEnvironmentPostProcessor.PREFER_JSON_FRAMEWORK_KEY, "jackson");

            new DubboJsonFrameworkEnvironmentPostProcessor()
                    .postProcessEnvironment(new StandardEnvironment(), new SpringApplication(Object.class));

            assertThat(System.getProperty(DubboJsonFrameworkEnvironmentPostProcessor.PREFER_JSON_FRAMEWORK_KEY))
                    .isEqualTo("jackson");
        } finally {
            restoreProperty(previous);
        }
    }

    private static void restoreProperty(String previous) {

        if (previous == null) {
            System.clearProperty(DubboJsonFrameworkEnvironmentPostProcessor.PREFER_JSON_FRAMEWORK_KEY);
        } else {
            System.setProperty(DubboJsonFrameworkEnvironmentPostProcessor.PREFER_JSON_FRAMEWORK_KEY, previous);
        }
    }
}
