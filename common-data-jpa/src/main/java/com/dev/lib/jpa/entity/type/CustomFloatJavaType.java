package com.dev.lib.jpa.entity.type;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.java.FloatJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * 自定义 Float 类型，修改默认 precision
 */
public class CustomFloatJavaType extends FloatJavaType {

    public static final CustomFloatJavaType INSTANCE = new CustomFloatJavaType();

    @Override
    public int getDefaultSqlPrecision(Dialect dialect, JdbcType jdbcType) {

        return TypeDefaults.FLOAT_PRECISION;
    }
}
