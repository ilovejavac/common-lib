package com.dev.lib.util.encrypt.condition;

import com.dev.lib.entity.encrypt.EncryptVersion;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Map;

class OnEncryptionStrategyCondition implements Condition {

    private static final String ENCRYPT_VERSION_PROPERTY = "app.security.encrypt-version";

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {

        Map<String, Object> attributes = metadata.getAnnotationAttributes(
                ConditionalOnEncryptionStrategy.class.getName()
        );
        if (attributes == null) {
            return false;
        }

        EncryptVersion version = (EncryptVersion) attributes.get("value");
        String encryptVersion = context.getEnvironment().getProperty(ENCRYPT_VERSION_PROPERTY);
        if (matches(version, encryptVersion)) {
            return true;
        }

        String configurationPrefix = configurationPrefix(version);
        Boolean enabled = context.getEnvironment().getProperty(
                configurationPrefix + ".enabled",
                Boolean.class
        );
        if (enabled != null) {
            return enabled;
        }

        return Binder.get(context.getEnvironment())
                .bind(configurationPrefix, Map.class)
                .isBound();
    }

    private boolean matches(EncryptVersion version, String candidate) {

        if (candidate == null || candidate.isBlank()) {
            return false;
        }
        String normalized = candidate.trim();
        return version.name().equalsIgnoreCase(normalized)
                || version.getMsg().equalsIgnoreCase(normalized);
    }

    private String configurationPrefix(EncryptVersion version) {

        return "app.security." + version.name().toLowerCase();
    }
}
