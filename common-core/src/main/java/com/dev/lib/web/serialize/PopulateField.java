package com.dev.lib.web.serialize;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.lang.annotation.*;

/**
 * 标记字段需要在序列化时额外输出填充对象
 *
 * 原字段保留，额外输出 {字段名}{suffix} 字段
 *
 * 示例：
 * @PopulateField(loader = "userLoader")
 * private Long creatorId;
 *
 * 输出：
 * {
 *     "creatorId": 123,
 *     "creatorIdInfo": { "username": "张三", ... }
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