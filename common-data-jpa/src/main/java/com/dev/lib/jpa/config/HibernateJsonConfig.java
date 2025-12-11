package com.dev.lib.jpa.config;

import com.alibaba.fastjson2.JSON;
import com.dev.lib.config.FastJson2Support;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.format.FormatMapper;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HibernateJsonConfig {

    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer(
//            ObjectMapper objectMapper
    ) {

        return properties -> properties.put(
                "hibernate.type.json_format_mapper",
//                new JacksonJsonFormatMapper(objectMapper)
                new FastJson2FormatMapper()
        );
    }

    static class FastJson2FormatMapper implements FormatMapper {

        @Override
        public <T> T fromString(
                CharSequence charSequence,
                JavaType<T> javaType,
                WrapperOptions wrapperOptions
        ) {

            if (charSequence == null) {
                return null;
            }
            return JSON.parseObject(
                    charSequence.toString(),
                    javaType.getJavaTypeClass(),
                    FastJson2Support.READER_FEATURES
            );
        }

        @Override
        public <T> String toString(
                T value,
                JavaType<T> javaType,
                WrapperOptions wrapperOptions
        ) {

            if (value == null) {
                return null;
            }
            return JSON.toJSONString(
                    value,
                    FastJson2Support.VALUE_FILTER,
                    FastJson2Support.WRITER_FEATURES
            );
        }

    }

}