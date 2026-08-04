package com.dev.lib.cloud.config;

import org.apache.dubbo.common.json.JsonUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.StandardEnvironment;

import java.util.Map;
import java.util.ServiceLoader;

import static org.assertj.core.api.Assertions.assertThat;

class DubboJsonFrameworkEnvironmentPostProcessorTest {

    @Test
    void shouldPreferJackson3ByDefault() {

        String previous = System.getProperty(DubboJsonFrameworkEnvironmentPostProcessor.PREFER_JSON_FRAMEWORK_KEY);
        try {
            System.clearProperty(DubboJsonFrameworkEnvironmentPostProcessor.PREFER_JSON_FRAMEWORK_KEY);

            new DubboJsonFrameworkEnvironmentPostProcessor()
                    .postProcessEnvironment(new StandardEnvironment(), new SpringApplication(Object.class));

            assertThat(System.getProperty(DubboJsonFrameworkEnvironmentPostProcessor.PREFER_JSON_FRAMEWORK_KEY))
                    .isEqualTo("jackson3");
        } finally {
            restoreProperty(previous);
        }
    }

    @Test
    void shouldProvideJackson3JsonUtilWithoutPolymorphicTypeLoading() {

        JsonUtil jsonUtil = ServiceLoader.load(JsonUtil.class).stream()
                .filter(provider -> provider.type().equals(Jackson3JsonUtil.class))
                .map(ServiceLoader.Provider::get)
                .findFirst()
                .orElseThrow();

        Object restored = jsonUtil.toJavaObject(
                "{\"@class\":\"java.lang.Runtime\",\"value\":7}",
                Object.class
        );

        assertThat(jsonUtil.getName()).isEqualTo("jackson3");
        assertThat(restored).isInstanceOf(Map.class);
        assertThat(((Map<?, ?>) restored).get("@class")).isEqualTo(Runtime.class.getName());
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
