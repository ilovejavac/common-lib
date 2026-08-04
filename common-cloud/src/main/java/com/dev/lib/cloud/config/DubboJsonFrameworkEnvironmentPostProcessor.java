package com.dev.lib.cloud.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

public class DubboJsonFrameworkEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String PREFER_JSON_FRAMEWORK_KEY = "dubbo.json-framework.prefer";

    private static final String DEFAULT_JSON_FRAMEWORK = "jackson3";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {

        if (System.getProperty(PREFER_JSON_FRAMEWORK_KEY) == null) {
            System.setProperty(PREFER_JSON_FRAMEWORK_KEY, DEFAULT_JSON_FRAMEWORK);
        }
    }

    @Override
    public int getOrder() {

        return Ordered.HIGHEST_PRECEDENCE;
    }
}
