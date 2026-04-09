package com.dev.lib.jpa.entity.type;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.java.DoubleJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * 自定义 Double 类型，修改默认 precision
 */
public class CustomDoubleJavaType extends DoubleJavaType {

    public static final CustomDoubleJavaType INSTANCE = new CustomDoubleJavaType();

    @Override
    public int getDefaultSqlPrecision(Dialect dialect, JdbcType jdbcType) {

        return TypeDefaults.DOUBLE_PRECISION;
    }
}
