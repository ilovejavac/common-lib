package com.dev.lib.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

import java.io.IOException;
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

    public static void configure(ObjectMapper mapper) {

        mapper.setTimeZone(java.util.TimeZone.getTimeZone(TIME_ZONE));
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        mapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);

        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
        mapper.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
        mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        mapper.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);
        mapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
        mapper.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);

        mapper.enable(JsonReadFeature.ALLOW_JAVA_COMMENTS.mappedFeature());
        mapper.enable(JsonReadFeature.ALLOW_SINGLE_QUOTES.mappedFeature());
        mapper.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
        mapper.enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);

        mapper.getFactory().setStreamReadConstraints(
                StreamReadConstraints.builder()
                        .maxNestingDepth(1000)
                        .maxNumberLength(1000)
                        .maxStringLength(20_000_000)
                        .build()
        );
        mapper.deactivateDefaultTyping();
        mapper.registerModule(commonModule());
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

    static class BigDecimalSerializer extends JsonSerializer<BigDecimal> {

        @Override
        public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider serializers) throws IOException {

            if (value == null) {
                gen.writeNull();
                return;
            }
            gen.writeNumber(value.setScale(6, RoundingMode.HALF_UP).toPlainString());
        }

        @Override
        public void serializeWithType(BigDecimal value, JsonGenerator gen, SerializerProvider serializers, TypeSerializer typeSer) throws IOException {

            WritableTypeId typeId = typeSer.writeTypePrefix(gen, typeSer.typeId(value, JsonToken.VALUE_NUMBER_FLOAT));
            serialize(value, gen, serializers);
            typeSer.writeTypeSuffix(gen, typeId);
        }
    }

    static class BigDecimalDeserializer extends JsonDeserializer<BigDecimal> {

        @Override
        public BigDecimal deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

            String text = p.getText();
            return text == null || text.isBlank() ? null : new BigDecimal(text.trim());
        }
    }

    static class LocalDateTimeSerializer extends JsonSerializer<LocalDateTime> {

        @Override
        public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {

            gen.writeString(value == null ? null : DATE_TIME_FORMATTER.format(value));
        }

        @Override
        public void serializeWithType(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers, TypeSerializer typeSer) throws IOException {

            WritableTypeId typeId = typeSer.writeTypePrefix(gen, typeSer.typeId(value, JsonToken.VALUE_STRING));
            serialize(value, gen, serializers);
            typeSer.writeTypeSuffix(gen, typeId);
        }
    }

    static class LocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

            String text = p.getText();
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

    static class LocalDateSerializer extends JsonSerializer<LocalDate> {

        @Override
        public void serialize(LocalDate value, JsonGenerator gen, SerializerProvider serializers) throws IOException {

            gen.writeString(value == null ? null : DATE_FORMATTER.format(value));
        }

        @Override
        public void serializeWithType(LocalDate value, JsonGenerator gen, SerializerProvider serializers, TypeSerializer typeSer) throws IOException {

            WritableTypeId typeId = typeSer.writeTypePrefix(gen, typeSer.typeId(value, JsonToken.VALUE_STRING));
            serialize(value, gen, serializers);
            typeSer.writeTypeSuffix(gen, typeId);
        }
    }

    static class LocalDateDeserializer extends JsonDeserializer<LocalDate> {

        @Override
        public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

            String text = p.getText();
            return text == null || text.isBlank() ? null : LocalDate.parse(text.trim(), DATE_FORMATTER);
        }
    }

    static class LocalTimeSerializer extends JsonSerializer<LocalTime> {

        @Override
        public void serialize(LocalTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {

            gen.writeString(value == null ? null : TIME_FORMATTER.format(value));
        }

        @Override
        public void serializeWithType(LocalTime value, JsonGenerator gen, SerializerProvider serializers, TypeSerializer typeSer) throws IOException {

            WritableTypeId typeId = typeSer.writeTypePrefix(gen, typeSer.typeId(value, JsonToken.VALUE_STRING));
            serialize(value, gen, serializers);
            typeSer.writeTypeSuffix(gen, typeId);
        }
    }

    static class LocalTimeDeserializer extends JsonDeserializer<LocalTime> {

        @Override
        public LocalTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

            String text = p.getText();
            return text == null || text.isBlank() ? null : LocalTime.parse(text.trim(), TIME_FORMATTER);
        }
    }

    static class InstantSerializer extends JsonSerializer<Instant> {

        @Override
        public void serialize(Instant value, JsonGenerator gen, SerializerProvider serializers) throws IOException {

            gen.writeString(value == null ? null : DATE_TIME_FORMATTER.format(value.atZone(ZONE_ID)));
        }

        @Override
        public void serializeWithType(Instant value, JsonGenerator gen, SerializerProvider serializers, TypeSerializer typeSer) throws IOException {

            WritableTypeId typeId = typeSer.writeTypePrefix(gen, typeSer.typeId(value, JsonToken.VALUE_STRING));
            serialize(value, gen, serializers);
            typeSer.writeTypeSuffix(gen, typeId);
        }
    }

    static class InstantDeserializer extends JsonDeserializer<Instant> {

        @Override
        public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

            String text = p.getText();
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

    static class LongSerializer extends JsonSerializer<Long> {

        @Override
        public void serialize(Long value, JsonGenerator gen, SerializerProvider serializers) throws IOException {

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
        public void serializeWithType(Long value, JsonGenerator gen, SerializerProvider serializers, TypeSerializer typeSer) throws IOException {

            JsonToken token = value != null && (value > JS_SAFE_INTEGER_MAX || value < -JS_SAFE_INTEGER_MAX)
                    ? JsonToken.VALUE_STRING
                    : JsonToken.VALUE_NUMBER_INT;
            WritableTypeId typeId = typeSer.writeTypePrefix(gen, typeSer.typeId(value, token));
            serialize(value, gen, serializers);
            typeSer.writeTypeSuffix(gen, typeId);
        }
    }
}
