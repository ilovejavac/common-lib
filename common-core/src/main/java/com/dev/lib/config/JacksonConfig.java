package com.dev.lib.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

@Configuration
public class JacksonConfig {

    private static final String TIME_ZONE = "Asia/Shanghai";
    private static final ZoneId ZONE_ID = ZoneId.of(TIME_ZONE);

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            // 自定义序列化器
            builder.serializerByType(BigDecimal.class, new BigDecimalSerializer());
            builder.deserializerByType(BigDecimal.class, new BigDecimalDeserializer());
            builder.serializerByType(LocalDateTime.class, new LocalDateTimeSerializer());
            builder.deserializerByType(LocalDateTime.class, new LocalDateTimeDeserializer());
            builder.serializerByType(LocalDate.class, new LocalDateSerializer());
            builder.deserializerByType(LocalDate.class, new LocalDateDeserializer());
            builder.serializerByType(Instant.class, new InstantSerializer());
            builder.deserializerByType(Instant.class, new InstantDeserializer());
            builder.timeZone(TimeZone.getTimeZone(TIME_ZONE));
            builder.serializationInclusion(JsonInclude.Include.NON_NULL);
            // 序列化配置
            builder.featuresToDisable(
                    SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
                    SerializationFeature.FAIL_ON_EMPTY_BEANS
            );
            builder.featuresToEnable(
                    SerializationFeature.WRITE_ENUMS_USING_TO_STRING
            );
            // 反序列化配置
            builder.featuresToDisable(
                    DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                    MapperFeature.DEFAULT_VIEW_INCLUSION  // 🔒 安全：防止视图泄露
            );
            builder.featuresToEnable(
                    DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS,
                    DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT,
                    DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY,
                    DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL,
                    DeserializationFeature.READ_ENUMS_USING_TO_STRING,
                    DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY,  // 🔒 安全：防止重复键攻击
                    MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS
            );
            // 流式配置
            builder.featuresToEnable(
                    JsonReadFeature.ALLOW_JAVA_COMMENTS.mappedFeature(),
                    JsonReadFeature.ALLOW_SINGLE_QUOTES.mappedFeature()
            );
            // 🔒 安全：检测重复 key + 限制解析深度
            builder.featuresToEnable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);

            builder.postConfigurer(mapper -> {
                mapper.enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);
                // 🔒 安全：限制反序列化深度和长度
                mapper.getFactory().setStreamReadConstraints(
                        StreamReadConstraints.builder()
                                .maxNestingDepth(1000)
                                .maxNumberLength(1000)
                                .maxStringLength(20_000_000)
                                .build()
                );

                // 🔒 安全：禁用默认多态类型处理
                mapper.deactivateDefaultTyping();
            });
        };
    }

    // ============ 序列化器 ============

    static class BigDecimalSerializer extends JsonSerializer<BigDecimal> {
        @Override
        public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value == null) {
                gen.writeNull();
            } else {
                gen.writeNumber(value.setScale(6, RoundingMode.HALF_UP).toPlainString());
            }
        }
    }

    static class BigDecimalDeserializer extends JsonDeserializer<BigDecimal> {
        @Override
        public BigDecimal deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String text = p.getText();
            return (text == null || text.isBlank()) ? null : new BigDecimal(text.trim());
        }
    }

    static class LocalDateTimeSerializer extends JsonSerializer<LocalDateTime> {
        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        @Override
        public void serialize(
                LocalDateTime value,
                JsonGenerator gen,
                SerializerProvider serializers
        ) throws IOException {
            gen.writeString(value == null ? null : FORMATTER.format(value));
        }
    }

    static class LocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String text = p.getText();
            if (text == null || text.isBlank()) return null;
            text = text.trim();
            // 兼容 ISO 格式
            if (text.contains("T")) {
                return LocalDateTime.parse(text);
            }
            return LocalDateTime.parse(text, FORMATTER);
        }
    }

    static class LocalDateSerializer extends JsonSerializer<LocalDate> {
        @Override
        public void serialize(LocalDate value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(value == null ? null : value.toString());
        }
    }

    static class LocalDateDeserializer extends JsonDeserializer<LocalDate> {
        @Override
        public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String text = p.getText();
            return (text == null || text.isBlank()) ? null : LocalDate.parse(text.trim());
        }
    }

    static class InstantSerializer extends JsonSerializer<Instant> {
        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        @Override
        public void serialize(Instant value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(value == null ? null : FORMATTER.format(value.atZone(ZONE_ID)));
        }
    }

    static class InstantDeserializer extends JsonDeserializer<Instant> {
        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        @Override
        public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String text = p.getText();
            if (text == null || text.isBlank()) return null;
            text = text.trim();
            if (text.contains("T")) {
                return Instant.parse(text);
            }
            return LocalDateTime.parse(text, FORMATTER).atZone(ZONE_ID).toInstant();
        }
    }
}