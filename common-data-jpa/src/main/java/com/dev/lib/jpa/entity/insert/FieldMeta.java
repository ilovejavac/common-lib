package com.dev.lib.jpa.entity.insert;

import com.alibaba.fastjson2.JSON;
import lombok.Data;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@Data
public class FieldMeta {

    private final Field field;

    private final String columnName;

    private final int sqlType;

    private final boolean isJson;

    private final boolean isEnumString;

    public Object getValue(Object entity) {

        try {
            Object value = field.get(entity);
            if (value == null) return null;

            if (isJson) {
                return JSON.toJSONString(value);
            }
            if (isEnumString && value instanceof Enum<?> e) {
                return e.name();
            }
            if (value instanceof LocalDateTime ldt) {
                return Timestamp.valueOf(ldt);
            }
            return value;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

}
