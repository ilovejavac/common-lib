package com.dev.lib.entity.log;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperateLog {

    String module();           // 模块

    String type();            // 操作类型

    String description();     // 描述

    boolean recordParams() default true;   // 记录参数

    boolean recordResult() default false;  // 记录返回值

}