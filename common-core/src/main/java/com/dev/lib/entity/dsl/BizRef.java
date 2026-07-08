package com.dev.lib.entity.dsl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Maps a query field value to the public id of an associated entity.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BizRef {

    /**
     * Marker value used when the association name should be inferred from the query field.
     */
    String FIELD_NAME = "__FIELD_NAME__";

    /**
     * Association field name, for example {@code goods} maps to {@code goods.bizId}.
     * Use {@code @BizRef} without a value to infer the association from the query field name.
     */
    String value() default FIELD_NAME;

}
