package com.dev.lib.storage.config.condition;

import com.dev.lib.storage.domain.model.StorageType;
import org.springframework.context.annotation.Conditional;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnResolvedStorageTypeCondition.class)
public @interface ConditionalOnResolvedStorageType {

    StorageType value();
}
