package com.dev.lib.jpa.config;

import com.dev.lib.util.Jsons;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.format.FormatMapper;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class HibernateJsonConfig {

    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer() {

        return properties -> properties.put(
                "hibernate.type.json_format_mapper",
                new JacksonFormatMapper()
        );
    }

    @Slf4j
    static class JacksonFormatMapper implements FormatMapper {

        @Override
        public <T> T fromString(
                CharSequence charSequence,
                JavaType<T> javaType,
                WrapperOptions wrapperOptions
        ) {

            if (charSequence == null) {
                return null;
            }
            try {
                return Jsons.parse(charSequence.toString(), javaType.getJavaTypeClass());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Failed to deserialize JSON for Hibernate type " + javaType.getJavaTypeClass().getName(),
                        e
                );
            }
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
            try {
                return Jsons.toJson(value);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Failed to serialize JSON for Hibernate value type " + value.getClass().getName(),
                        e
                );
            }
        }
    }
}
