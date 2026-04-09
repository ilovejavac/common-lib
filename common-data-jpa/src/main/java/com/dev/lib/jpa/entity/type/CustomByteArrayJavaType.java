package com.dev.lib.jpa.entity.type;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * 自定义 byte[] 类型，修改默认 varbinary 长度
 */
public class CustomByteArrayJavaType extends PrimitiveByteArrayJavaType {

    public static final CustomByteArrayJavaType INSTANCE = new CustomByteArrayJavaType();

    @Override
    public long getDefaultSqlLength(Dialect dialect, JdbcType jdbcType) {

        return TypeDefaults.BYTE_ARRAY_LENGTH;
    }
}
