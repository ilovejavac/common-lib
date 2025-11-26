package com.dev.lib.jpa.entity.type;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.java.InstantJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * 自定义 Instant 类型，修改默认时间戳精度
 */
public class CustomInstantJavaType extends InstantJavaType {

    public static final CustomInstantJavaType INSTANCE = new CustomInstantJavaType();

    @Override
    public int getDefaultSqlPrecision(Dialect dialect, JdbcType jdbcType) {
        return TypeDefaults.TIMESTAMP_PRECISION;
    }
}