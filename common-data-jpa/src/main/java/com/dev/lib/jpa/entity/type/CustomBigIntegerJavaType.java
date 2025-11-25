package com.dev.lib.jpa.entity.type;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.java.BigIntegerJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * 自定义 BigInteger 类型，修改默认 precision
 */
public class CustomBigIntegerJavaType extends BigIntegerJavaType {

    public static final CustomBigIntegerJavaType INSTANCE = new CustomBigIntegerJavaType();

    @Override
    public int getDefaultSqlPrecision(Dialect dialect, JdbcType jdbcType) {
        return TypeDefaults.BIG_INTEGER_PRECISION;
    }
}