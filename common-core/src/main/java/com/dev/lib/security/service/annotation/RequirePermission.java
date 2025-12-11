package com.dev.lib.security.service.annotation;

import java.lang.annotation.*;

/**
 * 校验用户权限
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {

    String[] value();

}