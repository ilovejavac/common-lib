package com.dev.lib.storage.config.condition;

import com.dev.lib.storage.config.AppStorageProperties;
import com.dev.lib.storage.domain.model.StorageType;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Map;

public class OnResolvedStorageTypeCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {

        Map<String, Object> attributes = metadata.getAnnotationAttributes(ConditionalOnResolvedStorageType.class.getName());
        if (attributes == null) {
            return false;
        }

        StorageType expectedType = (StorageType) attributes.get("value");
        BindResult<AppStorageProperties> bindResult = Binder.get(context.getEnvironment())
                .bind("app.storage", AppStorageProperties.class);
        AppStorageProperties properties = bindResult.orElseGet(AppStorageProperties::new);
        StorageType effectiveType = properties.getEffectiveType();
        return expectedType == effectiveType;
    }
}
