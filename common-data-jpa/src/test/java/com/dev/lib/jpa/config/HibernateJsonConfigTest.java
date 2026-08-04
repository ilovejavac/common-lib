package com.dev.lib.jpa.config;

import com.dev.lib.config.JacksonSupport;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.format.FormatMapper;
import org.hibernate.type.format.jackson.Jackson3JsonFormatMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HibernateJsonConfigTest {

    @Test
    void shouldInstallJackson3MapperUsingSpringManagedJsonMapper() throws Exception {

        JsonMapper jsonMapper = JacksonSupport.mapper();
        Method factoryMethod = Arrays.stream(HibernateJsonConfig.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("hibernatePropertiesCustomizer"))
                .filter(method -> Arrays.equals(method.getParameterTypes(), new Class<?>[]{JsonMapper.class}))
                .findFirst()
                .orElse(null);

        assertThat(factoryMethod).isNotNull();
        HibernatePropertiesCustomizer customizer = (HibernatePropertiesCustomizer) factoryMethod.invoke(
                new HibernateJsonConfig(),
                jsonMapper
        );
        Map<String, Object> properties = new HashMap<>();
        customizer.customize(properties);
        Object configuredMapper = properties.get("hibernate.type.json_format_mapper");

        assertThat(configuredMapper).isInstanceOf(Jackson3JsonFormatMapper.class);
        FormatMapper formatMapper = (FormatMapper) configuredMapper;
        JavaType<Payload> javaType = new PayloadJavaType();

        Payload payload = new Payload();
        payload.amount = new BigDecimal("9.1");
        payload.createdAt = Instant.parse("2026-03-11T04:05:06Z");

        String json = formatMapper.toString(payload, javaType, null);
        Payload restored = formatMapper.fromString(json, javaType, null);

        assertThat(json).contains("\"amount\":9.100000");
        assertThat(json).contains("\"createdAt\":\"2026-03-11 12:05:06\"");
        assertThat(restored.amount).isEqualByComparingTo("9.100000");
        assertThat(restored.createdAt).isEqualTo(Instant.parse("2026-03-11T04:05:06Z"));
    }

    static class Payload {

        public BigDecimal amount;

        public Instant createdAt;
    }

    static class PayloadJavaType implements JavaType<Payload> {

        @Override
        public Class<Payload> getJavaTypeClass() {

            return Payload.class;
        }

        @Override
        public JdbcType getRecommendedJdbcType(JdbcTypeIndicators context) {

            return null;
        }

        @Override
        public Payload fromString(CharSequence string) {

            throw new UnsupportedOperationException();
        }

        @Override
        public <X> X unwrap(Payload value, Class<X> type, WrapperOptions options) {

            throw new UnsupportedOperationException();
        }

        @Override
        public <X> Payload wrap(X value, WrapperOptions options) {

            throw new UnsupportedOperationException();
        }
    }
}
