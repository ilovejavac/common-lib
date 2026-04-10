package com.dev.lib.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Auto-loads common-lib default config files as low-priority property sources.
 * Business service config remains higher priority and can always override.
 */
public class CommonLibDefaultsEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String ENABLED_KEY = "app.common-lib.auto-config.enabled";

    private static final String SOURCE_PREFIX = "common-lib-auto-config:";

    private static final Set<String> SUPPORTED_CONFIG_FILES = Set.of(
            "application-lib.yml",
            "application-lib.yaml",
            "application-data.yml",
            "application-data.yaml",
            "application-storage.yml",
            "application-storage.yaml",
            "application-security.yml",
            "application-security.yaml",
            "application-search.yml",
            "application-search.yaml",
            "application-cloud.yml",
            "application-cloud.yaml",
            "application-mq.yml",
            "application-mq.yaml",
            "application-excel.yml",
            "application-excel.yaml"
    );

    private static final List<String> RESOURCE_PATTERNS = List.of(
            "classpath*:application-*.yml",
            "classpath*:application-*.yaml"
    );

    private final YamlPropertySourceLoader loader = new YamlPropertySourceLoader();

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {

        if (!environment.getProperty(ENABLED_KEY, Boolean.class, true)) {
            return;
        }

        MutablePropertySources propertySources = environment.getPropertySources();
        for (Resource resource : findResources()) {
            String fileName = resource.getFilename();
            if (fileName == null || !SUPPORTED_CONFIG_FILES.contains(fileName)) {
                continue;
            }
            if (!resource.exists()) {
                continue;
            }
            String sourceName = SOURCE_PREFIX + resource.getDescription();
            if (propertySources.contains(sourceName)) {
                continue;
            }
            try {
                List<PropertySource<?>> loadedSources = loader.load(sourceName, resource);
                for (PropertySource<?> loadedSource : loadedSources) {
                    propertySources.addLast(loadedSource);
                }
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to load common-lib default config: " + resource.getDescription(), ex);
            }
        }
    }

    private List<Resource> findResources() {

        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(getClass().getClassLoader());
        Set<String> seenDescriptions = new HashSet<>();
        List<Resource> resources = new ArrayList<>();

        for (String pattern : RESOURCE_PATTERNS) {
            try {
                for (Resource resource : resolver.getResources(pattern)) {
                    String description = resource.getDescription();
                    if (seenDescriptions.add(description)) {
                        resources.add(resource);
                    }
                }
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to scan resources with pattern: " + pattern, ex);
            }
        }

        resources.sort(Comparator.comparing(Resource::getDescription));
        return resources;
    }

    @Override
    public int getOrder() {

        return Ordered.LOWEST_PRECEDENCE;
    }
}
