package com.dev.lib.aksk.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Aksk {

    String[] scopes() default {};

    String headerPrefix() default "";

    String accessKeyHeader() default "";

    String timestampHeader() default "";

    String signatureHeader() default "";
}
