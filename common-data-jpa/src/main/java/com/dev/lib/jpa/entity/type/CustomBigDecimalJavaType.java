package com.dev.lib.jpa.entity.type;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.java.BigDecimalJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * 自定义 BigDecimal 类型，修改默认 precision 和 scale
 */
public class CustomBigDecimalJavaType extends BigDecimalJavaType {

    public static final CustomBigDecimalJavaType INSTANCE = new CustomBigDecimalJavaType();

    @Override
    public int getDefaultSqlPrecision(Dialect dialect, JdbcType jdbcType) {

        return TypeDefaults.DECIMAL_PRECISION;
    }

    @Override
    public int getDefaultSqlScale(Dialect dialect, JdbcType jdbcType) {

        return TypeDefaults.DECIMAL_SCALE;
    }

}