package com.dev.lib.mongo;

import com.querydsl.core.annotations.QueryEntity;
import org.springframework.core.annotation.AliasFor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.lang.annotation.*;

/**
 * MongoDB 实体注解，组合 @Document + @QueryEntity
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Document
@QueryEntity
public @interface MongoDocument {

    /**
     * 集合名称
     */
    @AliasFor(annotation = Document.class, attribute = "collection")
    String collection() default "";

    /**
     * 同 collection
     */
    @AliasFor(annotation = Document.class, attribute = "value")
    String value() default "";
}
