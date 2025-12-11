package com.dev.lib.jpa.entity.type;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.java.StringJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * 自定义 String 类型，修改默认 varchar 长度
 */
public class CustomStringJavaType extends StringJavaType {

    public static final CustomStringJavaType INSTANCE = new CustomStringJavaType();

    @Override
    public long getDefaultSqlLength(Dialect dialect, JdbcType jdbcType) {

        return TypeDefaults.STRING_LENGTH;
    }

}