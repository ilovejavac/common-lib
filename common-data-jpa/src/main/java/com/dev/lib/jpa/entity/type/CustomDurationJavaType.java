package com.dev.lib.jpa.entity.type;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.java.DurationJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * 自定义 Duration 类型，修改默认 numeric(21,9)
 */
public class CustomDurationJavaType extends DurationJavaType {

    public static final CustomDurationJavaType INSTANCE = new CustomDurationJavaType();

    @Override
    public int getDefaultSqlPrecision(Dialect dialect, JdbcType jdbcType) {

        return TypeDefaults.DURATION_PRECISION;
    }

    @Override
    public int getDefaultSqlScale(Dialect dialect, JdbcType jdbcType) {

        return TypeDefaults.DURATION_SCALE;
    }
}
