package com.dev.lib.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

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

    private static final String timeZone = "Asia/Shanghai";

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // === 基础配置 ===
        mapper.enable(JsonReadFeature.ALLOW_JAVA_COMMENTS.mappedFeature());
        mapper.enable(JsonReadFeature.ALLOW_SINGLE_QUOTES.mappedFeature());

        // === 数字处理 ===
        mapper.enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);
        mapper.configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true);
        mapper.disable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS);

        // === 日期时间 ===
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.registerModule(new JavaTimeModule());
        // 移除SimpleDateFormat，使用JavaTimeModule的默认格式
        mapper.setTimeZone(TimeZone.getTimeZone(timeZone));

        // === 容错处理 ===
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        mapper.enable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT);
        mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        mapper.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);
        mapper.enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY); // 防止重复key
        mapper.enable(SerializationFeature.FAIL_ON_SELF_REFERENCES); // 防止自引用

        // === null处理 ===
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // === 字符串 ===
        mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);

        // === 枚举 ===
        mapper.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
        mapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
        mapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);

        // === 自定义序列化器（保留4位小数）===
        SimpleModule module = new SimpleModule();

        // BigDecimal保留4位
        module.addSerializer(BigDecimal.class, new BigDecimalSerializer());
        module.addDeserializer(BigDecimal.class, new BigDecimalDeserializer());

        mapper.registerModule(module);

        // === 性能优化 ===
        mapper.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
        mapper.enable(JsonGenerator.Feature.ESCAPE_NON_ASCII);

        // === 默认类型信息（可选，用于多态类型）===
        mapper.setPolymorphicTypeValidator(LaissezFaireSubTypeValidator.instance);

        return mapper;
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            builder.mixIn(Object.class, BaseEntityMixIn.class);

            // Long转String
            builder.serializerByType(Long.class, new LongToStringSerializer());
            builder.serializerByType(Long.TYPE, new LongToStringSerializer());
            builder.deserializerByType(Long.class, new StringToLongDeserializer());
            builder.deserializerByType(Long.TYPE, new StringToLongDeserializer());

            // BigDecimal处理
            builder.serializerByType(BigDecimal.class, new BigDecimalSerializer());
            builder.deserializerByType(BigDecimal.class, new BigDecimalDeserializer());
            builder.featuresToEnable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);


            // 时间类型处理
            builder.modulesToInstall(new JavaTimeModule());

            builder.serializerByType(LocalDateTime.class, new LocalDateTimeSerializer());
            builder.deserializerByType(LocalDateTime.class, new LocalDateTimeDeserializer());

            builder.serializerByType(LocalDate.class, new LocalDateSerializer());
            builder.deserializerByType(LocalDate.class, new LocalDateDeserializer());

            builder.serializerByType(Instant.class, new InstantSerializer());
            builder.deserializerByType(Instant.class, new InstantDeserializer());

            builder.serializationInclusion(JsonInclude.Include.NON_NULL);
            builder.failOnUnknownProperties(false);
            builder.failOnEmptyBeans(false);
            builder.indentOutput(false);

            // 空值容错
            builder.featuresToEnable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
            builder.featuresToEnable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT);
            builder.featuresToEnable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
            builder.featuresToEnable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);

            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            builder.featuresToEnable(JsonParser.Feature.ALLOW_SINGLE_QUOTES);
            builder.featuresToEnable(JsonParser.Feature.ALLOW_COMMENTS);
            builder.featuresToEnable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
            builder.featuresToEnable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
            builder.featuresToEnable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);

            // 递归防护
            builder.featuresToEnable(SerializationFeature.FAIL_ON_SELF_REFERENCES);
            builder.featuresToEnable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);

            builder.postConfigurer(mapper ->
                    mapper.setTimeZone(java.util.TimeZone.getTimeZone(timeZone))
            );
        };
    }

    interface BaseEntityMixIn {
        @JsonIgnore
        Long getId();

        @JsonIgnore
        String getVersion();

        @JsonIgnore
        Boolean getDeleted();
    }

    // ============ 序列化器实现 ============

    static class LongToStringSerializer extends JsonSerializer<Long> {
        @Override
        public void serialize(Long value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value == null) {
                gen.writeNull();
            } else {
                gen.writeString(value.toString());
            }
        }
    }

    static class StringToLongDeserializer extends JsonDeserializer<Long> {
        @Override
        public Long deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String text = p.getText();
            if (text == null || text.isEmpty()) {
                return null;
            }
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException e) {
                return p.getLongValue();
            }
        }
    }

    static class BigDecimalSerializer extends JsonSerializer<BigDecimal> {
        @Override
        public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value == null) {
                gen.writeNull();
            } else {
                gen.writeNumber(value.setScale(4, RoundingMode.HALF_UP).toPlainString());
            }
        }
    }

    static class BigDecimalDeserializer extends JsonDeserializer<BigDecimal> {
        @Override
        public BigDecimal deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String text = p.getText();
            return (text == null || text.isEmpty()) ? null : new BigDecimal(text);
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
            if (value == null) {
                gen.writeNull();
            } else {
                gen.writeString(FORMATTER.format(value));
            }
        }
    }

    static class LocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
        private static final DateTimeFormatter[] FORMATTERS = {
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
                DateTimeFormatter.ISO_LOCAL_DATE_TIME
        };

        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String text = p.getText();
            if (text == null || text.isEmpty()) {
                return null;
            }

            for (DateTimeFormatter formatter : FORMATTERS) {
                try {
                    return LocalDateTime.parse(text, formatter);
                } catch (Exception ignored) {
                }
            }

            throw new IllegalArgumentException("Cannot parse LocalDateTime: " + text);
        }
    }

    static class LocalDateSerializer extends JsonSerializer<LocalDate> {
        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        @Override
        public void serialize(LocalDate value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value == null) {
                gen.writeNull();
            } else {
                gen.writeString(FORMATTER.format(value));
            }
        }
    }

    static class LocalDateDeserializer extends JsonDeserializer<LocalDate> {
        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        @Override
        public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String text = p.getText();
            return (text == null || text.isEmpty()) ? null : LocalDate.parse(text, FORMATTER);
        }
    }

    static class InstantSerializer extends JsonSerializer<Instant> {
        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        private static final ZoneId ASIA_SHANGHAI = ZoneId.of(timeZone);

        @Override
        public void serialize(Instant value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value == null) {
                gen.writeNull();
            } else {
                String formatted = FORMATTER.format(value.atZone(ASIA_SHANGHAI));
                gen.writeString(formatted);
            }
        }
    }

    static class InstantDeserializer extends JsonDeserializer<Instant> {
        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        private static final ZoneId ASIA_SHANGHAI = ZoneId.of(timeZone);

        @Override
        public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String text = p.getText();
            if (text == null || text.isEmpty()) {
                return null;
            }

            if (text.contains("T")) {
                return Instant.parse(text);
            } else {
                LocalDateTime localDateTime = LocalDateTime.parse(text, FORMATTER);
                return localDateTime.atZone(ASIA_SHANGHAI).toInstant();
            }
        }
    }
}