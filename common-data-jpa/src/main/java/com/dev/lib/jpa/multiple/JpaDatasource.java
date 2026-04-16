package com.dev.lib.jpa.multiple;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 声明一个 JPA 数据源，将指定 DataSource bean 与 entity/repository 包绑定。
 * 框架自动注册对应的 EntityManagerFactory、TransactionManager、JPAQueryFactory
 * 及该包下所有 JPA Repository。
 *
 * <pre>
 * &#64;Configuration
 * &#64;JpaDatasource(datasource = "primaryDs", packages = {"com.x.a", "com.x.b"})
 * &#64;JpaDatasource(datasource = "secondaryDs", packages = {"com.y.c"})
 * public class JpaConfig {
 *
 *     &#64;Bean &#64;Primary
 *     public DataSource primaryDs() { ... }
 *
 *     &#64;Bean
 *     public DataSource secondaryDs() { ... }
 * }
 * </pre>
 *
 * <p>第一个声明的数据源自动成为 &#64;Primary，对应生成：
 * <ul>
 *   <li>{datasource}EntityManagerFactory</li>
 *   <li>{datasource}TransactionManager</li>
 *   <li>{datasource}JpaQueryFactory</li>
 * </ul>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(JpaDatasource.Container.class)
@Import(JpaDatasourceRegistrar.class)
public @interface JpaDatasource {

    /** DataSource 的 bean 名称 */
    String datasource();

    /** entity 和 repository 所在包路径（同时用于 entity 扫描和 repository 扫描） */
    String[] packages();

    /** 方言类型；未设置时回退到全局 app.dialect / spring.jpa.database-platform */
    JpaDialect dialect() default JpaDialect.AUTO;

    /** 多个 &#64;JpaDatasource 时的容器注解，由 Java 自动使用，用户无需关心 */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @Import(JpaDatasourceRegistrar.class)
    @interface Container {
        JpaDatasource[] value();
    }
}
