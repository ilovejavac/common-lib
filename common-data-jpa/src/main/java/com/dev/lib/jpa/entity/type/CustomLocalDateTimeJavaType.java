package com.dev.lib.jpa.entity.type;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.java.LocalDateTimeJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * 自定义 LocalDateTime 类型，修改默认时间戳精度
 */
public class CustomLocalDateTimeJavaType extends LocalDateTimeJavaType {

    public static final CustomLocalDateTimeJavaType INSTANCE = new CustomLocalDateTimeJavaType();

    @Override
    public int getDefaultSqlPrecision(Dialect dialect, JdbcType jdbcType) {

        return TypeDefaults.TIMESTAMP_PRECISION;
    }

}