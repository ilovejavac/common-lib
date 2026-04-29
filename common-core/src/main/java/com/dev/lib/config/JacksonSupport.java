package com.dev.lib.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonToken;
import tools.jackson.core.JsonParser;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.core.type.WritableTypeId;
import tools.jackson.core.json.JsonFactoryBuilder;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.module.SimpleModule;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class JacksonSupport {

    public static final String TIME_ZONE = "Asia/Shanghai";

    public static final ZoneId ZONE_ID = ZoneId.of(TIME_ZONE);

    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT);

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final long JS_SAFE_INTEGER_MAX = 9007199254740991L;

    private JacksonSupport() {
    }

    public static void configure(JsonMapper.Builder builder) {

        builder.defaultTimeZone(java.util.TimeZone.getTimeZone(TIME_ZONE));
        builder.changeDefaultPropertyInclusion(ignored ->
                JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL));

        builder.disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS);
        builder.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        builder.enable(EnumFeature.WRITE_ENUMS_USING_TO_STRING);

        builder.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        builder.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
        builder.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
        builder.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        builder.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        builder.enable(EnumFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);
        builder.enable(EnumFeature.READ_ENUMS_USING_TO_STRING);
        builder.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);

        builder.enable(JsonReadFeature.ALLOW_JAVA_COMMENTS);
        builder.enable(JsonReadFeature.ALLOW_SINGLE_QUOTES);
        builder.enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION);
        builder.enable(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN);

        builder.deactivateDefaultTyping();
        builder.addModule(commonModule());
    }

    public static void configure(JsonFactoryBuilder builder) {

        builder.enable(JsonReadFeature.ALLOW_JAVA_COMMENTS);
        builder.enable(JsonReadFeature.ALLOW_SINGLE_QUOTES);
        builder.enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION);
        builder.enable(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN);
        builder.streamReadConstraints(
                StreamReadConstraints.builder()
                        .maxNestingDepth(1000)
                        .maxNumberLength(1000)
                        .maxStringLength(20_000_000)
                        .build()
        );
    }

    public static SimpleModule commonModule() {

        SimpleModule module = new SimpleModule("common-lib-jackson");
        module.addSerializer(BigDecimal.class, new BigDecimalSerializer());
        module.addDeserializer(BigDecimal.class, new BigDecimalDeserializer());
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer());
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer());
        module.addSerializer(LocalDate.class, new LocalDateSerializer());
        module.addDeserializer(LocalDate.class, new LocalDateDeserializer());
        module.addSerializer(LocalTime.class, new LocalTimeSerializer());
        module.addDeserializer(LocalTime.class, new LocalTimeDeserializer());
        module.addSerializer(Instant.class, new InstantSerializer());
        module.addDeserializer(Instant.class, new InstantDeserializer());
        module.addSerializer(Long.class, new LongSerializer());
        module.addSerializer(Long.TYPE, new LongSerializer());
        return module;
    }

    static class BigDecimalSerializer extends ValueSerializer<BigDecimal> {

        @Override
        public void serialize(BigDecimal value, JsonGenerator gen, SerializationContext serializers) throws JacksonException {

            if (value == null) {
                gen.writeNull();
                return;
            }
            gen.writeNumber(value.setScale(6, RoundingMode.HALF_UP).toPlainString());
        }

        @Override
        public void serializeWithType(
                BigDecimal value,
                JsonGenerator gen,
                SerializationContext serializers,
                TypeSerializer typeSer
        ) throws JacksonException {

            writeScalarWithType(value, JsonToken.VALUE_NUMBER_FLOAT, gen, serializers, typeSer, () ->
                    serialize(value, gen, serializers));
        }
    }

    static class BigDecimalDeserializer extends ValueDeserializer<BigDecimal> {

        @Override
        public BigDecimal deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {

            String text = p.getString();
            return text == null || text.isBlank() ? null : new BigDecimal(text.trim());
        }
    }

    static class LocalDateTimeSerializer extends ValueSerializer<LocalDateTime> {

        @Override
        public void serialize(LocalDateTime value, JsonGenerator gen, SerializationContext serializers) throws JacksonException {

            gen.writeString(value == null ? null : DATE_TIME_FORMATTER.format(value));
        }

        @Override
        public void serializeWithType(
                LocalDateTime value,
                JsonGenerator gen,
                SerializationContext serializers,
                TypeSerializer typeSer
        ) throws JacksonException {

            writeScalarWithType(value, JsonToken.VALUE_STRING, gen, serializers, typeSer, () ->
                    serialize(value, gen, serializers));
        }
    }

    static class LocalDateTimeDeserializer extends ValueDeserializer<LocalDateTime> {

        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {

            String text = p.getString();
            if (text == null || text.isBlank()) {
                return null;
            }
            text = text.trim();
            if (text.contains("T")) {
                return LocalDateTime.parse(text);
            }
            return LocalDateTime.parse(text, DATE_TIME_FORMATTER);
        }
    }

    static class LocalDateSerializer extends ValueSerializer<LocalDate> {

        @Override
        public void serialize(LocalDate value, JsonGenerator gen, SerializationContext serializers) throws JacksonException {

            gen.writeString(value == null ? null : DATE_FORMATTER.format(value));
        }

        @Override
        public void serializeWithType(
                LocalDate value,
                JsonGenerator gen,
                SerializationContext serializers,
                TypeSerializer typeSer
        ) throws JacksonException {

            writeScalarWithType(value, JsonToken.VALUE_STRING, gen, serializers, typeSer, () ->
                    serialize(value, gen, serializers));
        }
    }

    static class LocalDateDeserializer extends ValueDeserializer<LocalDate> {

        @Override
        public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {

            String text = p.getString();
            return text == null || text.isBlank() ? null : LocalDate.parse(text.trim(), DATE_FORMATTER);
        }
    }

    static class LocalTimeSerializer extends ValueSerializer<LocalTime> {

        @Override
        public void serialize(LocalTime value, JsonGenerator gen, SerializationContext serializers) throws JacksonException {

            gen.writeString(value == null ? null : TIME_FORMATTER.format(value));
        }

        @Override
        public void serializeWithType(
                LocalTime value,
                JsonGenerator gen,
                SerializationContext serializers,
                TypeSerializer typeSer
        ) throws JacksonException {

            writeScalarWithType(value, JsonToken.VALUE_STRING, gen, serializers, typeSer, () ->
                    serialize(value, gen, serializers));
        }
    }

    static class LocalTimeDeserializer extends ValueDeserializer<LocalTime> {

        @Override
        public LocalTime deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {

            String text = p.getString();
            return text == null || text.isBlank() ? null : LocalTime.parse(text.trim(), TIME_FORMATTER);
        }
    }

    static class InstantSerializer extends ValueSerializer<Instant> {

        @Override
        public void serialize(Instant value, JsonGenerator gen, SerializationContext serializers) throws JacksonException {

            gen.writeString(value == null ? null : DATE_TIME_FORMATTER.format(value.atZone(ZONE_ID)));
        }

        @Override
        public void serializeWithType(
                Instant value,
                JsonGenerator gen,
                SerializationContext serializers,
                TypeSerializer typeSer
        ) throws JacksonException {

            writeScalarWithType(value, JsonToken.VALUE_STRING, gen, serializers, typeSer, () ->
                    serialize(value, gen, serializers));
        }
    }

    static class InstantDeserializer extends ValueDeserializer<Instant> {

        @Override
        public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {

            String text = p.getString();
            if (text == null || text.isBlank()) {
                return null;
            }
            text = text.trim();
            if (text.length() > 100) {
                throw new IllegalArgumentException("Date string too long: " + text.length());
            }
            if (text.contains("T")) {
                return Instant.parse(text);
            }
            return LocalDateTime.parse(text, DATE_TIME_FORMATTER).atZone(ZONE_ID).toInstant();
        }
    }

    static class LongSerializer extends ValueSerializer<Long> {

        @Override
        public void serialize(Long value, JsonGenerator gen, SerializationContext serializers) throws JacksonException {

            if (value == null) {
                gen.writeNull();
                return;
            }
            if (value > JS_SAFE_INTEGER_MAX || value < -JS_SAFE_INTEGER_MAX) {
                gen.writeString(value.toString());
                return;
            }
            gen.writeNumber(value);
        }

        @Override
        public void serializeWithType(
                Long value,
                JsonGenerator gen,
                SerializationContext serializers,
                TypeSerializer typeSer
        ) throws JacksonException {

            JsonToken token = value != null && (value > JS_SAFE_INTEGER_MAX || value < -JS_SAFE_INTEGER_MAX)
                    ? JsonToken.VALUE_STRING
                    : JsonToken.VALUE_NUMBER_INT;
            writeScalarWithType(value, token, gen, serializers, typeSer, () ->
                    serialize(value, gen, serializers));
        }
    }

    private static void writeScalarWithType(
            Object value,
            JsonToken token,
            JsonGenerator gen,
            SerializationContext serializers,
            TypeSerializer typeSer,
            ScalarWriter writer
    ) throws JacksonException {

        WritableTypeId typeId = typeSer.writeTypePrefix(gen, serializers, typeSer.typeId(value, token));
        writer.write();
        typeSer.writeTypeSuffix(gen, serializers, typeId);
    }

    @FunctionalInterface
    private interface ScalarWriter {

        void write() throws JacksonException;
    }
}
