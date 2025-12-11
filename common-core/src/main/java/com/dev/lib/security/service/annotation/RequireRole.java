package com.dev.lib.security.service.annotation;

import java.lang.annotation.*;

/**
 * 校验用户身份
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {

    String[] value();

}