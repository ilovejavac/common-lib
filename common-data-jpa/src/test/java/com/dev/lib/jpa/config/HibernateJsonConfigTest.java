package com.dev.lib.jpa.config;

import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.descriptor.WrapperOptions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class HibernateJsonConfigTest {

    @Test
    void shouldRoundTripJsonUsingSharedJacksonRules() {

        HibernateJsonConfig.JacksonFormatMapper formatMapper = new HibernateJsonConfig.JacksonFormatMapper();
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
