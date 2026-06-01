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
     * Association field name, for example {@code goods} maps to {@code goods.bizId}.
     */
    String value();

}
