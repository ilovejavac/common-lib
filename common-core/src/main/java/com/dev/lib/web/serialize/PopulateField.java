package com.dev.lib.web.serialize;

import java.lang.annotation.*;

/**
 * 标记字段需要在序列化时额外输出填充对象
 * <p>
 * 原字段保留，额外输出 {字段名}{suffix} 字段
 * <p>
 * 示例：
 *
 * @PopulateField(loader = "userLoader")
 * private Long creatorId;
 * <p>
 * 输出：
 * {
 * "creatorId": 123,
 * "creatorIdInfo": { "username": "张三", ... }
 * }
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PopulateField {

    /**
     * 加载器名称，对应 Spring Bean 名称
     */
    String loader();

    /**
     * 填充字段后缀，默认 "Info"
     * 生成字段名 = 原字段名 + suffix
     */
    String suffix() default "Info";

}