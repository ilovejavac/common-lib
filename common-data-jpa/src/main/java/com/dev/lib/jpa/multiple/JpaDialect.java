package com.dev.lib.jpa.multiple;

import org.springframework.util.StringUtils;

/**
 * JPA 方言枚举。
 *
 * <p>设计原则：
 * <ul>
 *   <li>优先覆盖 common-lib 常见数据库</li>
 *   <li>对协议兼容数据库提供安全默认映射（如 Doris -> MySQL）</li>
 *   <li>对 Hibernate 无内置方言的数据库，要求显式指定 databasePlatform</li>
 * </ul>
 */
public enum JpaDialect {

    /** 自动：沿用 databasePlatform 或全局 spring.jpa.database-platform */
    AUTO(null),

    MYSQL("org.hibernate.dialect.MySQLDialect"),
    POSTGRESQL("org.hibernate.dialect.PostgreSQLDialect"),
    ORACLE("org.hibernate.dialect.OracleDialect"),
    H2("org.hibernate.dialect.H2Dialect"),
    SQLITE("org.hibernate.community.dialect.SQLiteDialect"),
    SQLSERVER("org.hibernate.dialect.SQLServerDialect"),
    MARIADB("org.hibernate.dialect.MariaDBDialect"),
    HANA("org.hibernate.dialect.HANADialect"),

    /**
     * Doris 默认走 MySQL 协议兼容方言。
     */
    DORIS("org.hibernate.dialect.MySQLDialect"),

    /**
     * GaussDB(openGauss 系)默认走 PostgreSQL 协议兼容方言。
     */
    GAUSSDB("org.hibernate.community.dialect.GaussDBDialect"),

    /**
     * openGauss 默认走 PostgreSQL 协议兼容方言。
     */
    OPENGAUSS("org.hibernate.community.dialect.GaussDBDialect"),

    CLICKHOUSE(null),
    HIVE(null),
    DM("org.hibernate.dialect.DmDialect"),
    KINGBASE("org.hibernate.dialect.Kingbase8Dialect"),
    HDB("org.hibernate.dialect.HANADialect");

    private final String defaultPlatform;

    JpaDialect(String defaultPlatform) {
        this.defaultPlatform = defaultPlatform;
    }

    /**
     * 解析最终方言：
     * 1) dialect 固定映射（若存在）
     * 2) databasePlatform（显式覆盖）
     * 3) globalPlatform（全局 spring.jpa.database-platform）
     */
    public String resolveDatabasePlatform(String databasePlatform, String globalPlatform) {

        if (StringUtils.hasText(defaultPlatform)) {
            return defaultPlatform;
        }
        if (this == AUTO) {
            if (StringUtils.hasText(databasePlatform)) {
                return databasePlatform;
            }
            return globalPlatform;
        }
        if (StringUtils.hasText(databasePlatform)) {
            return databasePlatform;
        }
        throw new IllegalArgumentException(
                "@JpaDatasource dialect=" + name() + " requires databasePlatform to be set"
        );
    }
}
